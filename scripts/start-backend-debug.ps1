$ErrorActionPreference = 'Stop'
$repo = 'D:\Java\train_spring\VeTau-v1'
$out = Join-Path $repo 'backend-cache-debug-current.out.log'
if (Test-Path $out) {
    Remove-Item $out -Force
}
Set-Location $repo
mvn -f 'D:\Java\train_spring\VeTau-v1\vetautet-start\pom.xml' org.springframework.boot:spring-boot-maven-plugin:run *> $out
