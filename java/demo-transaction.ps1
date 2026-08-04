# Demo: @Transactional lam gi.
# Chay app truoc (.\mvnw.cmd spring-boot:run), roi chay file nay o cua so PowerShell KHAC.

$base = 'http://localhost:8080'

function ShowLedger([string]$nhan) {
    $l = Invoke-RestMethod "$base/demo/ledger"
    "{0,-22} alice={1,8}  bob={2,8}  TONG={3,8}" -f $nhan, $l.alice, $l.bob, $l.total
}

''
'Bean TransferService thuc su la class gi:'
'  ' + (Invoke-RestMethod "$base/demo/bean-class")

''
'==================================================================='
'  KICH BAN 1 - KHONG co @Transactional'
'==================================================================='
Invoke-RestMethod "$base/demo/reset" -Method Post | Out-Null
ShowLedger 'truoc khi chuyen'

$ket = Invoke-RestMethod "$base/demo/transfer-without-transaction" -Method Post
"  ket qua: $ket"

ShowLedger 'sau khi chuyen'

''
'==================================================================='
'  KICH BAN 2 - CO @Transactional  (than ham y het)'
'==================================================================='
Invoke-RestMethod "$base/demo/reset" -Method Post | Out-Null
ShowLedger 'truoc khi chuyen'

$ket = Invoke-RestMethod "$base/demo/transfer-with-transaction" -Method Post
"  ket qua: $ket"

ShowLedger 'sau khi chuyen'
''
