const http = require('http');

const data = JSON.stringify({ email: "admin@polleria.com", password: "Admin123!" });

const req = http.request({
  hostname: 'localhost',
  port: 8080,
  path: '/api/auth/login',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
}, (res) => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => {
    const token = JSON.parse(body).token;
    
    // Now update Pedido 67 to PAGADO
    const patchData = JSON.stringify({ nuevoEstado: "PAGADO", observacion: "Fijado desde script" });
    const req2 = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/pedidos/67/estado',
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token,
        'Content-Length': patchData.length
      }
    }, res2 => {
      let b2 = '';
      res2.on('data', d => b2 += d);
      res2.on('end', () => console.log('UPDATE STATUS:', res2.statusCode, b2));
    });
    req2.write(patchData);
    req2.end();
  });
});
req.write(data);
req.end();
