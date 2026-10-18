"""Assemble review APKs using an installed Android SDK, without an AGP download.
The normal Android Studio/Gradle project remains the primary development entrypoint.
Usage: python scripts/assemble_sdk.py --sdk /path/to/sdk --out /path/to/build
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import argparse,subprocess,zipfile,xml.etree.ElementTree as ET,hashlib,json,os,re

ROOT=Path(__file__).resolve().parents[1]
ARTIFACTS=[
 ('androidx.recyclerview','recyclerview','1.3.2','aar'),
 ('androidx.core','core','1.7.0','aar'),
 ('androidx.customview','customview','1.0.0','aar'),
 ('androidx.customview','customview-poolingcontainer','1.0.0','aar'),
 ('androidx.collection','collection','1.0.0','jar'),
 ('androidx.annotation','annotation','1.2.0','jar'),
 ('androidx.annotation','annotation-experimental','1.1.0','aar'),
 ('androidx.concurrent','concurrent-futures','1.0.0','jar'),
 ('androidx.interpolator','interpolator','1.0.0','aar'),
 ('androidx.lifecycle','lifecycle-runtime','2.3.1','aar'),
 ('androidx.lifecycle','lifecycle-common','2.3.1','jar'),
 ('androidx.arch.core','core-common','2.1.0','jar'),
 ('androidx.arch.core','core-runtime','2.1.0','aar'),
 ('androidx.versionedparcelable','versionedparcelable','1.1.1','aar'),
 ('org.jetbrains.kotlin','kotlin-stdlib','1.8.22','jar'),
 ('org.jetbrains','annotations','13.0','jar'),
 ('com.google.guava','listenablefuture','1.0','jar')]

def run(args):
    subprocess.run([str(a) for a in args],check=True)
def jar(source,dest):
    with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED) as z:
        for f in source.rglob('*.class'):z.write(f,f.relative_to(source))
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--sdk',required=True,type=Path);parser.add_argument('--out',required=True,type=Path);opts=parser.parse_args();sdk=opts.sdk.resolve();out=opts.out.resolve();out.mkdir(parents=True,exist_ok=True);deps=out/'deps';deps.mkdir(exist_ok=True)
    def fetch(item):
        group,name,version,ext=item;base='https://dl.google.com/dl/android/maven2/' if group.startswith('androidx.') else 'https://repo.maven.apache.org/maven2/';url=base+group.replace('.','/')+'/'+name+'/'+version+'/'+name+'-'+version+'.'+ext;f=deps/(name+'-'+version+'.'+ext)
        if not f.exists():run(['curl','-fsSL','--retry','2','--max-time','60',url,'-o',f])
        with zipfile.ZipFile(f) as z:
            if z.testzip():raise ValueError('Invalid dependency archive')
        return {'file':f.name,'url':url,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()}
    with ThreadPoolExecutor(max_workers=8) as pool:lock=list(pool.map(fetch,ARTIFACTS))
    (out/'dependency-hashes.json').write_text(json.dumps(lock,indent=2))
    tool=sdk/'build-tools/34.0.0';android=sdk/'platforms/android-34/android.jar';aapt=tool/'aapt2';compiled=[];libraries=[];packages=[]
    for f in sorted(deps.iterdir()):
        if f.suffix=='.jar':libraries.append(f);continue
        if f.suffix!='.aar':continue
        directory=deps/f.stem;directory.mkdir(exist_ok=True)
        with zipfile.ZipFile(f) as z:z.extractall(directory)
        libraries.append(directory/'classes.jar')
        manifest=ET.parse(directory/'AndroidManifest.xml').getroot();packages.append(manifest.attrib['package'])
        if (directory/'res').exists():
            resource=deps/(f.stem+'-res.zip');run([aapt,'compile','--dir',directory/'res','-o',resource]);compiled.append(resource)
    resource=out/'app-res.zip';run([aapt,'compile','--dir',ROOT/'app/src/main/res','-o',resource]);compiled.append(resource)
    ns='{http://schemas.android.com/apk/res/android}';ET.register_namespace('android',ns[1:-1])
    manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml');root=manifest.getroot();root.set('package','com.example.login');root.set(ns+'versionCode','2');root.set(ns+'versionName','2.0');root.find('application').set(ns+'debuggable','true');ET.SubElement(root,'uses-sdk',{ns+'minSdkVersion':'26',ns+'targetSdkVersion':'34'})
    for a in root.find('application').findall('activity'):
        if a.attrib[ns+'name'].startswith('.'):a.set(ns+'name','com.example.login'+a.attrib[ns+'name'])
    manifest.write(out/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
    generated=out/'generated';generated.mkdir(exist_ok=True);unsigned=out/'resources.apk'
    args=[aapt,'link','-I',android,'--manifest',out/'AndroidManifest.xml','--rename-manifest-package','com.zakk.travel','--auto-add-overlay','--java',generated,'--extra-packages',':'.join(sorted(set(packages))),'-o',unsigned]
    for f in compiled:args+=['-R',f]
    run(args)
    classes=out/'classes';classes.mkdir(exist_ok=True);cp=os.pathsep.join(str(p) for p in [android]+libraries)
    sources=list((ROOT/'app/src/main/java').rglob('*.java'))+list(generated.rglob('*.java'))
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-g','-cp',cp,'-d',classes]+sources)
    appjar=out/'app.jar';jar(classes,appjar);dex=out/'dex';dex.mkdir(exist_ok=True)
    run(['java','-cp',tool/'lib/d8.jar','com.android.tools.r8.D8','--debug','--min-api','26','--lib',android,'--output',dex,appjar]+libraries)
    with zipfile.ZipFile(unsigned,'a',zipfile.ZIP_DEFLATED) as z:
        for f in dex.glob('*.dex'):z.write(f,f.name)
    key=out/'review.keystore'
    if not key.exists():run(['keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','review','-dname','CN=Zakk Travel Review','-keyalg','RSA','-keysize','2048','-validity','3650'])
    def sign(source,target):
        aligned=out/(target.stem+'-aligned.apk');run([tool/'zipalign','-f','4',source,aligned]);run(['java','-jar',tool/'lib/apksigner.jar','sign','--ks',key,'--ks-pass','pass:android','--key-pass','pass:android','--out',target,aligned]);run(['java','-jar',tool/'lib/apksigner.jar','verify',target])
    sign(unsigned,out/'ZakkTravel_v2_Debug.apk')
    test=out/'test';test.mkdir(exist_ok=True);(test/'AndroidManifest.xml').write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.zakk.travel.test"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="34"/><application android:label="Zakk checks" android:debuggable="true"/><instrumentation android:name="com.example.login.SmokeTests" android:targetPackage="com.zakk.travel"/></manifest>')
    run([aapt,'link','-I',android,'--manifest',test/'AndroidManifest.xml','-o',test/'resources.apk'])
    tc=test/'classes';tc.mkdir(exist_ok=True)
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-g','-cp',cp+os.pathsep+str(classes),'-d',tc]+list((ROOT/'app/src/androidTest/java').rglob('*.java')))
    tj=test/'test.jar';jar(tc,tj);td=test/'dex';td.mkdir(exist_ok=True)
    args=['java','-cp',tool/'lib/d8.jar','com.android.tools.r8.D8','--debug','--min-api','26','--lib',android,'--classpath',appjar]
    for lib in libraries:args+=['--classpath',lib]
    run(args+['--output',td,tj])
    with zipfile.ZipFile(test/'resources.apk','a',zipfile.ZIP_DEFLATED) as z:
        for f in td.glob('*.dex'):z.write(f,f.name)
    sign(test/'resources.apk',out/'ZakkTravel_v2_Tests.apk')
    print('APK BUILD AND SIGNATURE VERIFICATION PASSED',flush=True)
if __name__=='__main__':main()
