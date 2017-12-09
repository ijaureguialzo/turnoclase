// Initialize Cloud Firestore through Firebase
var db = firebase.firestore();

// Iniciar sesión anónima
firebase.auth().signInAnonymously().catch(function (error) {
    // Handle Errors here.
    var errorCode = error.code;
    var errorMessage = error.message;

    console.log("Error de conexión: " + errorMessage);
});

// Comprobar si ha tenido éxito
firebase.auth().onAuthStateChanged(function (user) {
    if (user) {

        // User is signed in.
        var isAnonymous = user.isAnonymous;
        var uid = user.uid;

        console.log("Sesión iniciada: " + uid);

    } else {
        // User is signed out.
    }
});

// TODO: Si se pulsa dos veces en el botón Ver, el listener se lanza dos veces
$('#aula-boton').on("click", function () {

    db.collection("aulas").where("codigo", "==", $('#aula-input').val())
        .onSnapshot(function (querySnapshot) {

            // Limpiar las listas
            $("#listaprimero").empty();
            $("#lista").empty();

            querySnapshot.forEach(function (doc) {

                var aula = doc.data();
                console.log(aula["codigo"] + ": " + aula["cola"].length);

                // Actualizar el recuento
                $("#recuento").text(aula["cola"].length);

                if (aula["cola"].length > 0) {

                    // Mostrar el primero
                    db.collection("alumnos").doc(aula["cola"][0]).get().then(function (doc) {
                        if (doc.exists) {
                            $("#listaprimero").append('<li class="list-group-item amarillo my-3">' + doc.data()["nombre"] + '</li>');
                        } else {
                            console.log("No such document!");
                        }
                    }).catch(function (error) {
                        console.log("Error getting document:", error);
                    });

                    // Si hay más, mostrarlos
                    for (var i = 1, len = aula["cola"].length; i < len; i++) {

                        db.collection("alumnos").doc(aula["cola"][i]).get().then(function (doc) {
                            if (doc.exists) {
                                $("#lista").append('<li class="list-group-item">' + doc.data()["nombre"] + '</li>');
                            } else {
                                console.log("No such document!");
                            }
                        }).catch(function (error) {
                            console.log("Error getting document:", error);
                        });

                    }

                }

            });

        });

});

// REF: Quitar el foco del botón: https://stackoverflow.com/a/23444942/5136913
$(".btn").mouseup(function () {
    $(this).blur();
})