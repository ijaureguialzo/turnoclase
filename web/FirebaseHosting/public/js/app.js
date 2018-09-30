// Configurar Firestores
const db = firebase.firestore();
const settings = {timestampsInSnapshots: true};
db.settings(settings);

// Iniciar sesión anónima
firebase.auth().signInAnonymously().catch(function (error) {
    console.log("Error de conexión: " + error.message);
});

// Comprobar si ha tenido éxito
firebase.auth().onAuthStateChanged(function (user) {
    if (user) {
        console.log("Sesión iniciada: " + user.uid);
    } else {
        console.log("No hay sesión de usuario")
    }
});

var actualizando = false;

// Responder al click del botón y conectar el listener a la cola para obtener actualizaciones
$('#aula-boton').on("click", function () {

    console.log("Botón pulsado");

    // Desactivar los botones para que el listener no se cargue dos veces
    $('#aula-boton').prop('disabled', true);
    $('#aula-input').prop('disabled', true);

    console.log("Aula: " + $('#aula-input').val());

    // Recuperar el aula
    db.collection("aulas").where("codigo", "==", $('#aula-input').val().toUpperCase()).limit(1).get()
        .then(snapshot => {
            snapshot.forEach(doc => {

                // Obtener la cola ordenada
                db.collection("aulas").doc(doc.id)
                    .collection("cola")
                    .orderBy("timestamp")
                    .onSnapshot(querySnapshot => {

                        if (!this.actualizando) {
                            this.actualizando = true;

                            // Actualizar el recuento
                            let recuento = querySnapshot.size;
                            $("#recuento").text(recuento);

                            console.log("Recuento: " + recuento);

                            // El primero se visualiza en amarillo
                            var primero = true;

                            $("#listaprimero").empty()
                            $("#lista").empty();

                            querySnapshot.forEach(doc => {

                                // Cargar los datos de cada alumno
                                db.collection("alumnos").doc(doc.data()["alumno"]).get().then(doc => {

                                    if (doc.exists && primero === true) {
                                        primero = false;
                                        $("#listaprimero").empty().append('<li class="list-group-item amarillo my-3">' + doc.data()["nombre"] + '</li>');
                                    } else if (doc.exists) {
                                        $("#lista").append('<li class="list-group-item">' + doc.data()["nombre"] + '</li>');
                                    } else {
                                        console.log("El alumno no existe");
                                    }

                                }).catch(error => {
                                    console.log("Error al recuperar datos:", error);
                                });
                            });

                            this.actualizando = false;
                        }
                    }, err => {
                        console.log(`Error: ${err}`);
                    });
            });
        })
        .catch(err => {
            console.log('Error al recuperar datos', err);
        });
});

// REF: Quitar el foco del botón: https://stackoverflow.com/a/23444942/5136913
$(".btn").mouseup(function () {
    $(this).blur();
})
