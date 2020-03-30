// REF: Configuración: https://cloud.google.com/firestore/docs/quickstart
const admin = require('firebase-admin');
const functions = require('firebase-functions');

// Librería para llamar a otras funciones
const fetch = require('node-fetch');

// Inicializar la aplicación
admin.initializeApp(functions.config().firebase);

// Obtener la referencia a Firestore
let db = admin.firestore();

// REF: Generador de IDs únicos: https://hashids.org
const Hashids = require('hashids/cjs');

// Función obsoleta, solo la usan los clientes sin actualizar
exports.crearAula = functions.firestore
    .document('aulas/{userId}')
    .onCreate((snap, context) => {

        let userId = context.params.userId;

        let codigo = snap.data().codigo;
        console.log('Recibido código:' + codigo);

        if (userId !== 'keepalive' && codigo === undefined) {
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
        } else {
            console.log('Keep me alive...');
        }

        return null;
    });

// Generar el código de aula y retornarlo
exports.nuevoCodigo = functions.https.onCall((data, context) => {

    const keepalive = data.keepalive;

    if (!keepalive) {
        const hashids = new Hashids("turnoclase", 5, "123456789ABCDEFGHIJKLNPQRSTUVXYZ");

        const refContador = db.collection('total').doc('aulas');

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
    } else {
        console.log('Keep me alive...');
    }
});

// Mantener cargadas las funciones, llamándolas cada 2 minutos
exports.keepalive = functions.pubsub
    .schedule('every 2 minutes')
    .onRun((context) => {

        let data = {
            codigo: '$$$$$'
        };

        db.collection('aulas').doc('keepalive').delete().then(function () {
            db.collection('aulas').doc('keepalive').set(data);
        });

        callCloudFunction('nuevoCodigo', {keepalive: true});

        return null;
    });

const callCloudFunction = async (functionName, data) => {

    let url = `https://us-central1-${functions.firebaseConfig().projectId}.cloudfunctions.net/${functionName}`;

    await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({data}),
    })
};
