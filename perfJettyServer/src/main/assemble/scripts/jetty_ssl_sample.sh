mkdir jetty_ssl
cd jetty_ssl/
openssl genrsa -aes128 -out jetty.key
openssl req -new -x509 -newkey rsa:2048 -sha256 -key jetty.key -out jetty.crt
openssl req -new -key jetty.key -out jetty.csr
keytool -keystore keystore -import -alias jetty -file jetty.crt -trustcacerts
openssl pkcs12 -inkey jetty.key -in jetty.crt -export -out jetty.pkcs12
keytool -importkeystore -srckeystore jetty.pkcs12 -srcstoretype PKCS12 -destkeystore keystore