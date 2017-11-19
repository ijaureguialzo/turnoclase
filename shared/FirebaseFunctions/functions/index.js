// REF: https://firebase.google.com/docs/functions/firestore-events?hl=es-419

var firestore = require('firebase-functions/lib/providers/firestore');

exports.crearAula = firestore
    .document('aulas/{userId}')
    .onCreate(event => {

    var token = "BE131"

    var codigo = event.data.data().codigo;

    if (codigo != "BE131") {
        
    // REF: https://github.com/sehrope/node-rand-token
    var randtoken = require('rand-token').generator({
        chars: '1234567890ABCDEF'
    });

    token = randtoken.generate(5);
    }

    return event.data.ref.set({
        codigo: token
    }, {merge: true});

});
