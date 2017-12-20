// REF: Configuración: https://cloud.google.com/firestore/docs/quickstart
const functions = require('firebase-functions');

// REF: https://github.com/sehrope/node-rand-token
var randtoken = require('rand-token').generator({
    chars: '1234567890ABCDEF'
});

exports.crearAula = functions.firestore
    .document('aulas/{userId}')
    .onCreate(event => {

        var token = randtoken.generate(5);

        return event.data.ref.set({
            codigo: token
        }, {merge: true});
    });
