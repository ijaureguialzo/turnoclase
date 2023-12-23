// REF: Configuración: https://cloud.google.com/firestore/docs/quickstart
const admin = require('firebase-admin');
const functions = require('firebase-functions');

// Inicializar la aplicación
admin.initializeApp(functions.config().firebase);

// Obtener la referencia a Firestore
let db = admin.firestore();

// REF: Generador de IDs únicos: https://hashids.org
const Hashids = require('hashids/cjs');

// Generar el código de aula y retornarlo
exports.nuevoCodigo = functions
    .region('europe-west1')
    .runWith({
        enforceAppCheck: true,
    })
    .https.onCall((data, context) => {

        // Verificar que haya un token de App Check válidos y, si no, retornar un error 401
        if (context.app === undefined) {
            throw new functions.https.HttpsError('failed-precondition', 'The function must be called from an App Check verified app.')
        }

        const hashids = new Hashids("turnoclase", 5, "123456789ABCDEFGHIJKLNPQRSTUVXYZ");

        const refContador = db.collection('total').doc('aulas'); // Max: 234255 (9RRRR)

        let contador = 0;

        // Incrementar el contador en una transacción y usarlo para generar el código
        return db.runTransaction(t => {
            return t.get(refContador)
                .then(doc => {
                    contador = doc.data().contador + 1;
                    t.update(refContador, {contador: contador});
                });
        }).then(result => {
            console.log('Nuevo código generado.');
            return {
                codigo: hashids.encode(contador)
            };
        }).catch(err => {
            console.log('Error al generar el código:', err);
            return {
                codigo: "?"
            };
        });
    });
