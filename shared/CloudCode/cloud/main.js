Parse.Cloud.define("nuevoCodigoAula", function(request, response) {

  function guid() {
    function s4() {
      return Math.floor((1 + Math.random()) *
        0x10000).toString(16).substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' +
      s4() + '-' + s4() + '-' + s4() + s4() + s4();
  }

  function comprobarCodigo(codigo) {
    var query = new Parse.Query("Aula");
    query.equalTo("codigo", codigo);
    query.first({
      success: function(object) {
        if (object) {
          codigo = guid().substring(0, 5).toUpperCase();
          console.log("Repetido");
          comprobarCodigo(codigo);
        } else {
          console.log("Unico");
          response.success(codigo);
        }
      },
      error: function(error) {}
    });
  }

  var codigo = guid().substring(0, 5).toUpperCase();
  comprobarCodigo(codigo);

});


Parse.Cloud.define("actualizarAula", function(request, response) {
  var remitente = request.user;
  var codigoAula = request.params.codigoAula;

  var caducidad = new Date();
  caducidad.setMinutes(caducidad.getMinutes() + 10);

  Parse.Push.send({
    channels: [ codigoAula ],
    expiration_time: caducidad,
    data: {
      "alert": "",
      "content-available": "1",
      "sound": "default"
    }
  }, {
    success: function() {
      response.success("OK");
    },
    error: function(error) {
      response.error("ERROR");
    }
  });
});
