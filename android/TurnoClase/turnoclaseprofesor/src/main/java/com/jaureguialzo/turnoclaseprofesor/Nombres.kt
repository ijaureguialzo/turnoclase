package com.jaureguialzo.turnoclaseprofesor

import java.util.*

/**
 * Created by widemos on 19/9/17.
 */

class Nombres {

    // REF: Nombres más frecuentes: http://www.ine.es/dyngs/INEbase/es/operacion.htm?c=Estadistica_C&cid=1254736177009&menu=ultiDatos&idp=1254734710990

    private val nombres_es = arrayOf("Jose", "Antonio", "Juan", "Manuel", "Francisco", "Luis", "Javier", "Miguel", "Angel", "Carlos", "Jesus", "David", "Pedro", "Daniel", "Maria", "Alejandro", "Rafael", "Fernando", "Alberto", "Pablo", "Ramon", "Jorge", "Sergio", "Enrique", "Vicente", "Andrés", "Diego", "Victor", "Ignacio", "Adrian", "Alvaro", "Raul", "Eduardo", "Ivan", "Joaquin", "Oscar", "Ruben", "Santiago", "Roberto", "Alfonso", "Mario", "Jaime", "Gabriel", "Ricardo", "Emilio", "Julio", "Marcos", "Salvador", "Tomas", "Julian", "Guillermo", "Jordi", "Agustin", "Felix", "Hugo", "Josep", "Nicolas", "Gonzalo", "Martin", "Mohamed", "Joan", "Cristian", "Cesar", "Marc", "Domingo", "Sebastian", "Felipe", "Alfredo", "Ismael", "Samuel", "Mariano", "Hector", "Aitor", "Esteban", "Xavier", "Gregorio", "Rodrigo", "Lucas", "Alex", "Iker", "Lorenzo", "Arturo", "Eugenio", "Cristobal", "Albert", "Borja", "Alexander", "Marco", "Valentin", "Adolfo", "German", "Jonathan", "Christian", "Isaac", "Ernesto", "Aaron", "Joel", "Mateo", "Dario", "Pau", "María", "Carmen", "Ana", "Isabel", "Dolores", "Pilar", "Teresa", "Josefa", "Rosa", "Cristina", "Angeles", "Antonia", "Laura", "Elena", "Francisca", "Marta", "Mercedes", "Luisa", "Concepcion", "Lucia", "Rosario", "Jose", "Paula", "Sara", "Juana", "Manuela", "Raquel", "Jesus", "Eva", "Beatriz", "Rocío", "Patricia", "Victoria", "Julia", "Encarnación", "Belen", "Silvia", "Andrea", "Esther", "Nuria", "Montserrat", "Alba", "Angela", "Irene", "Inmaculada", "Monica", "Sandra", "Yolanda", "Alicia", "Sonia", "Mar", "Margarita", "Marina", "Susana", "Natalia", "Amparo", "Claudia", "Nieves", "Gloria", "Inés", "Carolina", "Soledad", "Veronica", "Lourdes", "Sofia", "Luz", "Noelia", "Begoña", "Lorena", "Carla", "Consuelo", "Asuncion", "Alejandra", "Olga", "Daniela", "Milagros", "Esperanza", "Fatima", "Catalina", "Blanca", "Miriam", "Nerea", "Lidia", "Aurora", "Clara", "Emilia", "Magdalena", "Celia", "Anna", "Elisa", "Eugenia", "Virginia", "Vanesa", "Adriana", "Josefina", "Purificación", "Gema", "Remedios", "Ainhoa", "Trinidad")

    // REF: Baby names in England and Wales: https://www.ons.gov.uk/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/bulletins/babynamesenglandandwales/2015

    private val nombres_en = arrayOf("Oliver", "Jack", "Harry", "George", "Jacob", "Charlie", "Noah", "William", "Thomas", "Oscar", "James", "Muhammad", "Henry", "Alfie", "Leo", "Joshua", "Freddie", "Ethan", "Archie", "Isaac", "Joseph", "Alexander", "Samuel", "Daniel", "Logan", "Edward", "Lucas", "Max", "Mohammed", "Benjamin", "Mason", "Harrison", "Theo", "Jake", "Sebastian", "Finley", "Arthur", "Adam", "Dylan", "Riley", "Zachary", "Teddy", "David", "Toby", "Theodore", "Elijah", "Matthew", "Jenson", "Jayden", "Harvey", "Reuben", "Harley", "Luca", "Michael", "Hugo", "Lewis", "Frankie", "Luke", "Stanley", "Tommy", "Jude", "Blake", "Louie", "Nathan", "Gabriel", "Charles", "Bobby", "Mohammad", "Ryan", "Tyler", "Elliott", "Albert", "Elliot", "Rory", "Alex", "Frederick", "Ollie", "Louis", "Dexter", "Jaxon", "Liam", "Jackson", "Callum", "Ronnie", "Leon", "Kai", "Aaron", "Roman", "Austin", "Ellis", "Jamie", "Reggie", "Seth", "Carter", "Felix", "Ibrahim", "Sonny", "Kian", "Caleb", "Connor", "Amelia", "Olivia", "Emily", "Isla", "Ava", "Ella", "Jessica", "Isabella", "Mia", "Poppy", "Sophie", "Sophia", "Lily", "Grace", "Evie", "Scarlett", "Ruby", "Chloe", "Isabelle", "Daisy", "Freya", "Phoebe", "Florence", "Alice", "Charlotte", "Sienna", "Matilda", "Evelyn", "Eva", "Millie", "Sofia", "Lucy", "Elsie", "Imogen", "Layla", "Rosie", "Maya", "Esme", "Elizabeth", "Lola", "Willow", "Ivy", "Erin", "Holly", "Emilia", "Molly", "Ellie", "Jasmine", "Eliza", "Lilly", "Abigail", "Georgia", "Maisie", "Eleanor", "Hannah", "Harriet", "Amber", "Bella", "Thea", "Annabelle", "Emma", "Amelie", "Harper", "Gracie", "Rose", "Summer", "Martha", "Violet", "Penelope", "Anna", "Nancy", "Zara", "Maria", "Darcie", "Maryam", "Megan", "Darcey", "Lottie", "Mila", "Heidi", "Lexi", "Lacey", "Francesca", "Robyn", "Bethany", "Julia", "Sara", "Aisha", "Darcy", "Zoe", "Clara", "Victoria", "Beatrice", "Hollie", "Arabella", "Sarah", "Maddison", "Leah", "Katie", "Aria")

    fun aleatorio(): String {

        val r = Random()

        var nombre: String

        val n: Int

        // REF: Obtener el idioma: https://stackoverflow.com/a/23168383/5136913

        if (Locale.getDefault().language.equals("es", ignoreCase = true)
                || Locale.getDefault().language.equals("eu", ignoreCase = true)) {
            n = r.nextInt(nombres_es.size)
            nombre = nombres_es[n]
        } else {
            n = r.nextInt(nombres_en.size)
            nombre = nombres_en[n]
        }

        return nombre
    }
}
