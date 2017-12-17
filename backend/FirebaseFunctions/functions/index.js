// REF: Configuración: https://cloud.google.com/firestore/docs/quickstart
const admin = require('firebase-admin');
const functions = require('firebase-functions');

admin.initializeApp(functions.config().firebase);

var db = admin.firestore();

// REF: https://github.com/sehrope/node-rand-token
var randtoken = require('rand-token').generator({
    chars: '1234567890ABCDEF'
});

// REF: https://firebase.google.com/docs/functions/firestore-events?hl=es-419

function nuevoCodigoAula(ref, token) {

    db.collection("aulas").where("codigo", "==", token)
        .get().then(function (querySnapshot) {
        if (querySnapshot.empty) {
            console.log("Nueva aula");
            ref.set({
                codigo: token
            }, {merge: true});
        } else {
            console.log("Aula repetida");
            nuevoCodigoAula(ref, randtoken.generate(5));
        }
    });

}

exports.crearAula = functions.firestore
    .document('aulas/{userId}')
    .onCreate(event => {
        nuevoCodigoAula(event.data.ref, randtoken.generate(5));
        return true;
    });
