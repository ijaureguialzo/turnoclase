// REF: Configuración: https://cloud.google.com/firestore/docs/quickstart
const admin = require('firebase-admin');
const functions = require('firebase-functions');

// Inicializar la aplicación
admin.initializeApp(functions.config().firebase);

// Obtener la referencia a Firestore
let db = admin.firestore();

// REF: Generador de IDs únicos: https://hashids.org
const Hashids = require('hashids/cjs');

exports.crearAula = functions.firestore
    .document('aulas/{userId}')
    .onCreate((snap, context) => {

        const hashids = new Hashids("turnoclase", 5, "123456789ABCDEFGHIJKLNPQRSTUVXYZ");

        const refContador = db.collection('total').doc('aulas');

        let contador = 0;

        // Incrementar el contador en una transacción y usarlo para generar el código
        let transaction = db.runTransaction(t => {
            return t.get(refContador)
                .then(doc => {
                    contador = doc.data().contador + 1;
                    t.update(refContador, {contador: contador});
                });
        }).then(result => {
            console.log('Transaction success!');

            return snap.ref.set({
                codigo: hashids.encode(contador)
            }, {merge: true});
        }).catch(err => {
            console.log('Transaction failure:', err);

            return snap.ref.set({
                codigo: "?"
            }, {merge: true});
        });
    });
