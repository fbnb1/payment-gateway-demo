# Demo: chứng minh self-invocation lọt qua proxy.
# Chạy app trước (.\mvnw.cmd spring-boot:run) rồi chạy script này ở cửa sổ PowerShell KHÁC.

$base = 'http://localhost:8080'
$body = '{"amount":100.50,"currency":"VND"}'

function Show([string]$label) {
    $s = Invoke-RestMethod -Uri "$base/payments/stats"
    "{0,-34} real={1}  proxy={2}  lot-qua={3}" -f $label, $s.realCreateCalls, $s.proxyIntercepted, $s.bypassedProxy
}

''
'Bean nam trong container:'
'  ' + (Invoke-RestMethod -Uri "$base/payments/bean-class")
''

Invoke-RestMethod -Uri "$base/payments/stats/reset" -Method Post | Out-Null
Show 'bat dau'

Invoke-RestMethod -Uri "$base/payments" -Method Post -ContentType 'application/json' -Body $body | Out-Null
Show 'sau POST /payments'

Invoke-RestMethod -Uri "$base/payments/twice" -Method Post -ContentType 'application/json' -Body $body | Out-Null
Show 'sau POST /payments/twice'

''
$s = Invoke-RestMethod -Uri "$base/payments/stats"
"create() chay that su : $($s.realCreateCalls) lan"
"proxy chan duoc       : $($s.proxyIntercepted) lan"
"LOT QUA proxy         : $($s.bypassedProxy) lan   <-- neu proxy la @Transactional,"
"                                                      day la so giao dich KHONG co transaction"
''
