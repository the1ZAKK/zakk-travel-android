from pathlib import Path
import tempfile,re,subprocess
root=Path(__file__).resolve().parents[1];src=root/'app/src/main/java/com/example/login'
with tempfile.TemporaryDirectory(prefix='zakk-checks-') as tmp:
    folder=Path(tmp);drawables=sorted(set(re.findall(r'R\.drawable\.(\w+)',(src/'Catalog.java').read_text())))
    generated=folder/'R.java';generated.write_text('package com.example.login;public final class R {public static final class drawable {'+''.join('public static final int '+n+'='+str(i+1)+';' for i,n in enumerate(drawables))+'}}')
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-d',tmp,str(generated)]+[str(src/(n+'.java')) for n in ['Rules','PasswordHasher','Catalog']]+[str(root/'tests/CoreChecks.java')],check=True)
    subprocess.run(['java','-cp',tmp,'com.example.login.CoreChecks'],check=True)
