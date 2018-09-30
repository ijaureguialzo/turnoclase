package com.jaureguialzo.turnoclase

import java.util.*

/**
 * Created by widemos on 19/9/17.
 */

class Nombres {

    // REF: Nombres más frecuentes: http://www.ine.es/dyngs/INEbase/es/operacion.htm?c=Estadistica_C&cid=1254736177009&menu=ultiDatos&idp=1254734710990
    private val nombres_es = arrayOf("Jose", "Antonio", "Juan", "Manuel", "Francisco", "Luis", "Javier", "Miguel", "Angel", "Carlos", "Jesus", "David", "Pedro", "Daniel", "Maria", "Alejandro", "Rafael", "Fernando", "Alberto", "Pablo", "Ramon", "Jorge", "Sergio", "Enrique", "Vicente", "Andrés", "Diego", "Victor", "Ignacio", "Adrian", "Alvaro", "Raul", "Eduardo", "Ivan", "Joaquin", "Oscar", "Ruben", "Santiago", "Roberto", "Alfonso", "Mario", "Jaime", "Gabriel", "Ricardo", "Emilio", "Julio", "Marcos", "Salvador", "Tomas", "Julian", "Guillermo", "Jordi", "Agustin", "Felix", "Hugo", "Josep", "Nicolas", "Gonzalo", "Martin", "Mohamed", "Joan", "Cristian", "Cesar", "Marc", "Domingo", "Sebastian", "Felipe", "Alfredo", "Ismael", "Samuel", "Mariano", "Hector", "Aitor", "Esteban", "Xavier", "Gregorio", "Rodrigo", "Lucas", "Alex", "Iker", "Lorenzo", "Arturo", "Eugenio", "Cristobal", "Albert", "Borja", "Alexander", "Marco", "Valentin", "Adolfo", "German", "Jonathan", "Christian", "Isaac", "Ernesto", "Aaron", "Joel", "Mateo", "Dario", "Pau", "María", "Carmen", "Ana", "Isabel", "Dolores", "Pilar", "Teresa", "Josefa", "Rosa", "Cristina", "Angeles", "Antonia", "Laura", "Elena", "Francisca", "Marta", "Mercedes", "Luisa", "Concepcion", "Lucia", "Rosario", "Jose", "Paula", "Sara", "Juana", "Manuela", "Raquel", "Jesus", "Eva", "Beatriz", "Rocío", "Patricia", "Victoria", "Julia", "Encarnación", "Belen", "Silvia", "Andrea", "Esther", "Nuria", "Montserrat", "Alba", "Angela", "Irene", "Inmaculada", "Monica", "Sandra", "Yolanda", "Alicia", "Sonia", "Mar", "Margarita", "Marina", "Susana", "Natalia", "Amparo", "Claudia", "Nieves", "Gloria", "Inés", "Carolina", "Soledad", "Veronica", "Lourdes", "Sofia", "Luz", "Noelia", "Begoña", "Lorena", "Carla", "Consuelo", "Asuncion", "Alejandra", "Olga", "Daniela", "Milagros", "Esperanza", "Fatima", "Catalina", "Blanca", "Miriam", "Nerea", "Lidia", "Aurora", "Clara", "Emilia", "Magdalena", "Celia", "Anna", "Elisa", "Eugenia", "Virginia", "Vanesa", "Adriana", "Josefina", "Purificación", "Gema", "Remedios", "Ainhoa", "Trinidad")

    // REF: Baby names in England and Wales: https://www.ons.gov.uk/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/bulletins/babynamesenglandandwales/2015
    private val nombres_en = arrayOf("Oliver", "Jack", "Harry", "George", "Jacob", "Charlie", "Noah", "William", "Thomas", "Oscar", "James", "Muhammad", "Henry", "Alfie", "Leo", "Joshua", "Freddie", "Ethan", "Archie", "Isaac", "Joseph", "Alexander", "Samuel", "Daniel", "Logan", "Edward", "Lucas", "Max", "Mohammed", "Benjamin", "Mason", "Harrison", "Theo", "Jake", "Sebastian", "Finley", "Arthur", "Adam", "Dylan", "Riley", "Zachary", "Teddy", "David", "Toby", "Theodore", "Elijah", "Matthew", "Jenson", "Jayden", "Harvey", "Reuben", "Harley", "Luca", "Michael", "Hugo", "Lewis", "Frankie", "Luke", "Stanley", "Tommy", "Jude", "Blake", "Louie", "Nathan", "Gabriel", "Charles", "Bobby", "Mohammad", "Ryan", "Tyler", "Elliott", "Albert", "Elliot", "Rory", "Alex", "Frederick", "Ollie", "Louis", "Dexter", "Jaxon", "Liam", "Jackson", "Callum", "Ronnie", "Leon", "Kai", "Aaron", "Roman", "Austin", "Ellis", "Jamie", "Reggie", "Seth", "Carter", "Felix", "Ibrahim", "Sonny", "Kian", "Caleb", "Connor", "Amelia", "Olivia", "Emily", "Isla", "Ava", "Ella", "Jessica", "Isabella", "Mia", "Poppy", "Sophie", "Sophia", "Lily", "Grace", "Evie", "Scarlett", "Ruby", "Chloe", "Isabelle", "Daisy", "Freya", "Phoebe", "Florence", "Alice", "Charlotte", "Sienna", "Matilda", "Evelyn", "Eva", "Millie", "Sofia", "Lucy", "Elsie", "Imogen", "Layla", "Rosie", "Maya", "Esme", "Elizabeth", "Lola", "Willow", "Ivy", "Erin", "Holly", "Emilia", "Molly", "Ellie", "Jasmine", "Eliza", "Lilly", "Abigail", "Georgia", "Maisie", "Eleanor", "Hannah", "Harriet", "Amber", "Bella", "Thea", "Annabelle", "Emma", "Amelie", "Harper", "Gracie", "Rose", "Summer", "Martha", "Violet", "Penelope", "Anna", "Nancy", "Zara", "Maria", "Darcie", "Maryam", "Megan", "Darcey", "Lottie", "Mila", "Heidi", "Lexi", "Lacey", "Francesca", "Robyn", "Bethany", "Julia", "Sara", "Aisha", "Darcy", "Zoe", "Clara", "Victoria", "Beatrice", "Hollie", "Arabella", "Sarah", "Maddison", "Leah", "Katie", "Aria")

    // REF: http://www.eustat.eus/elementos/ele0005700/Lista_de_los_100_nombres_de_nina_mas_frecuentes_en_la_CA_de_Euskadi/tbl0005715_c.html
    // REF: http://www.eustat.eus/elementos/ele0005700/Lista_de_los_100_nombres_de_nino_mas_frecuentes_en_la_CA_de_Euskadi/tbl0005716_c.html
    private val nombres_eu = arrayOf("Jon", "Markel", "Julen", "Ibai", "Aimar", "Ander", "Oier", "Unax", "Mikel", "Martin/Martín", "Oihan", "Unai", "Iker", "Danel", "Amets", "Izei", "Eneko", "Xabier", "Hugo", "Alain", "Hodei", "Aritz", "Aner", "Beñat", "Ekain", "Álex", "Luka", "Luken", "Izan", "Asier", "Telmo", "Aiur", "Ian", "Daniel", "Adrian/Adrián", "Erik", "Peio", "Lucas", "Ekhi", "Enaitz", "Lier", "Eder", "Manex", "Paul", "Aratz", "Peru", "Adei", "Leo", "Ekaitz", "Pablo", "Aitor", "Lander", "Mateo", "Urko", "Eñaut", "Ager", "David", "Oinatz", "Iñigo", "Adam", "Diego", "Liher", "Irai", "Ibon", "Enzo", "Gorka", "Marcos", "Mario", "Xuban", "Alejandro", "Álvaro", "Andoni", "Dylan", "Ekai", "Nicolás", "Eki", "Endika", "Inhar", "Marco", "Adur", "Haritz", "Hegoi", "Jokin", "Luca", "Omar", "Aingeru", "Rayan", "Thiago", "Jakes", "Mohamed", "Zuhaitz", "Gaizka", "Gari", "Inar", "Ion", "Ivan", "Josu", "Kimetz", "Liam", "Alexander", "Ane", "June", "Nahia", "Irati", "Laia", "Noa", "Nora", "Lucía", "Maddi", "Uxue", "Haizea", "Malen", "Izaro", "Sara", "Paula", "Martina", "Maialen", "Alaia", "Elene", "Enara", "Iraia", "Leire", "Libe", "Naia", "Alba", "Jare", "Maren", "Sofia/Sofía", "Udane", "Aitana", "Mara", "Emma", "Aiala", "Naroa", "Paule", "Maria/María", "Daniela", "Garazi", "Nerea", "Lur", "Elaia", "Anne", "Eider", "Irene", "Ariane", "Izar", "Maider", "Izadi", "Ainhize", "Adriana", "Jone", "Olaia", "Amaia", "Aroa", "Nikole", "Chloe", "Maia", "Olivia", "Ainara", "Alazne", "Carla", "Laida", "Ilargi", "Zoe", "Alaitz", "Valeria", "Aiora", "Alize", "Lea", "Vera", "Miren", "Ainhoa", "Iria", "Sare", "Julia", "Laura", "Oihane", "Inés", "Arhane", "Mia", "Saioa", "Alma", "Araitz", "Intza", "Iraide", "Luna", "Vega", "Aya", "Claudia", "Lara", "Maitane", "Olatz", "Elena", "Ixone", "Leize", "Lia", "Marwa", "Naiara", "Arane", "Katalin")

    fun aleatorio(): String {

        val r = Random()
        val n: Int

        // REF: Obtener el idioma: https://stackoverflow.com/a/23168383/5136913

        return when {
            Locale.getDefault().language.equals("es", ignoreCase = true) -> {
                n = r.nextInt(nombres_es.size)
                nombres_es[n]
            }
            Locale.getDefault().language.equals("eu", ignoreCase = true) -> {
                n = r.nextInt(nombres_eu.size)
                nombres_eu[n]
            }
            else -> {
                n = r.nextInt(nombres_en.size)
                nombres_en[n]
            }
        }
    }
}
