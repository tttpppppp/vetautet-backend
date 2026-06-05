# k6 load test

Script mặc định:

- [trips-popular.js](D:/Java/train_spring/VeTau-v1/scripts/load-test/trips-popular.js)

PowerShell runner:

- [run-k6.ps1](D:/Java/train_spring/VeTau-v1/scripts/load-test/run-k6.ps1)

## Chạy nhanh

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test\run-k6.ps1
```

Mặc định script sẽ test:

- `GET http://localhost:8080/api/v1/trips/popular?limit=6`
- `50` VUs
- `30s`

## Ví dụ đổi tham số

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test\run-k6.ps1 `
  -BaseUrl "http://localhost:8080" `
  -ApiPath "/api/v1/trips/popular?limit=6" `
  -Vus 100 `
  -Duration "60s"
```

## Nếu `k6` không nằm trong PATH

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test\run-k6.ps1 `
  -K6Path "C:\tools\k6\k6.exe"
```

## Output

Kết quả sẽ được lưu trong:

- `target/k6/<timestamp>/stdout.txt`
- `target/k6/<timestamp>/summary.json`
