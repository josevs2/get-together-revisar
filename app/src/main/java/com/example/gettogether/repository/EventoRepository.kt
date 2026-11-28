package com.example.gettogether.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gettogether.R
import com.example.gettogether.model.Evento
import com.example.gettogether.model.EventoDto
import com.example.gettogether.model.EventoForm
import com.example.gettogether.model.FiltrosBusqueda
import com.example.gettogether.model.Mensaje
import com.example.gettogether.service.XanoRetrofit

/**
 * Fuente de datos de eventos (en memoria, igual que UsuarioRepository de basic-app).
 * Como usa mutableStateOf, las pantallas que leen estos datos se recomponen solas.
 */
object EventoRepository {

    // titulo/categoria = nombre/tipo, porque Buscar y Detalle muestran esos campos (vienen así desde Xano)
    private var _eventos by mutableStateOf(eventosIniciales().map { it.copy(titulo = it.nombre, categoria = it.tipo) })
    val eventos: List<Evento> get() = _eventos

    private var _mensajes by mutableStateOf(mensajesIniciales())
    private var nextId = 100

    fun obtenerEvento(id: Int): Evento? = _eventos.find { it.id == id }

    /** Eventos a los que el usuario todavía no se une (feed de Inicio). */
    fun eventosDisponibles(usuario: String): List<Evento> =
        _eventos.filter { usuario !in it.asistentes }

    /** Eventos confirmados por el usuario (Bandeja). */
    fun eventosConfirmados(usuario: String): List<Evento> =
        _eventos.filter { usuario in it.asistentes }

    fun crearEvento(form: EventoForm, creador: String): Evento {
        val evento = Evento(
            id = nextId++,
            nombre = form.nombre.trim(),
            tipo = form.tipo,
            descripcion = form.descripcion.trim(),
            ubicacion = form.ubicacion.trim(),
            etiquetas = form.etiquetas.split(",")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotEmpty() },
            edadMinima = form.edadMinima.toInt(),
            edadMaxima = form.edadMaxima.toInt(),
            cupo = form.cupo.toInt(),
            asistentes = listOf(creador),
            imagenes = form.imagenes,
            creador = creador,
            titulo = form.nombre.trim(),
            categoria = form.tipo,
        )
        _eventos = listOf(evento) + _eventos
        return evento
    }

    fun confirmarAsistencia(id: Int, usuario: String) {
        _eventos = _eventos.map { e ->
            if (e.id == id && usuario !in e.asistentes && e.cuposDisponibles > 0)
                e.copy(asistentes = e.asistentes + usuario)
            else e
        }
    }

    fun cancelarAsistencia(id: Int, usuario: String) {
        _eventos = _eventos.map { e ->
            if (e.id == id) e.copy(asistentes = e.asistentes - usuario) else e
        }
    }

    fun mensajes(id: Int): List<Mensaje> = _mensajes[id].orEmpty()

    fun enviarMensaje(id: Int, autor: String, texto: String) {
        _mensajes = _mensajes + (id to (mensajes(id) + Mensaje(autor, texto)))
    }

    fun buscar(f: FiltrosBusqueda): List<Evento> = _eventos.filter { e ->
        val etiqueta = f.etiqueta.trim().removePrefix("#")
        (f.texto.isBlank() || e.nombre.contains(f.texto.trim(), true) || e.descripcion.contains(f.texto.trim(), true)) &&
                (f.ubicacion.isBlank() || e.ubicacion.contains(f.ubicacion.trim(), true) || e.referencia.contains(f.ubicacion.trim(), true)) &&
                (f.tipo == null || e.tipo == f.tipo) &&
                e.cupo <= f.cupoMaximo &&
                e.asistentes.size >= f.minConfirmados.toInt() &&
                // el rango del evento se cruza con el rango buscado
                e.edadMinima <= f.edadHasta && e.edadMaxima >= f.edadDesde &&
                (etiqueta.isEmpty() || e.etiquetas.any { it.contains(etiqueta, true) })
    }

    // ---------- Datos de ejemplo (sacados del mockup) ----------
    // Para cada evento puedes completar:
    //   latitud / longitud  → coordenadas del mapa (ej: latitud = -33.4489, longitud = -70.6693)
    //   imagenesRes         → imágenes en res/drawable (ej: listOf(R.drawable.pokemon_1, R.drawable.pokemon_2))

    private fun eventosIniciales() = listOf(
        Evento(
            id = 1,
            nombre = "Pokemon Go",
            tipo = "Juegos",
            descripcion = "Reunión pokemon incursiones. Traigan batería externa.",
            ubicacion = "San Joaquín",
            referencia = "Metro Macul",
            etiquetas = listOf("pokemon", "incursiones", "juegos"),
            asistentes = listOf("juan", "luis", "sofia", "nicolas"),
            portadaRes = R.drawable.pokemon_portada,
            imagenesRes = listOf(R.drawable.pokemon_1),
            latitud = -33.50931324626522,
            longitud = -70.58944158971403,                       // tarjeta
            creador = "juan", // Ver info
        ),
        Evento(
            id = 2, nombre = "Pichanga de los jueves", tipo = "Deporte",
            descripcion = "Fútbol 7, faltan jugadores. Nivel relajado.",
            ubicacion = "Ñuñoa", referencia = "Estadio Nacional",
            etiquetas = listOf("futbol", "pichanga"),
            cupo = 14, asistentes = listOf("matias", "felipe", "diego"),
            portadaRes = R.drawable.pichanga_portada, imagenesRes = listOf(R.drawable.pichanga_2),
            latitud = -33.464625017366615,                       // tarjeta
            longitud = -70.61078671991382, // Ver info
            creador = "matias",
        ),
        Evento(
            id = 3, nombre = "Noche de juegos de mesa", tipo = "Juegos",
            descripcion = "Catan, Dixit y lo que traigan. Hay picoteo.",
            ubicacion = "Providencia", referencia = "Metro Manuel Montt",
            etiquetas = listOf("juegos de mesa", "catan"),
            edadMinima = 20, edadMaxima = 35, cupo = 8,
            asistentes = listOf("camila", "ignacio"),
            portadaRes = R.drawable.juegos_portada, imagenesRes = listOf(R.drawable.juegos_1),
            latitud = -33.4282784176981,                       // tarjeta
            longitud = -70.6194141419454, // Ver info
            creador = "camila",
        ),
        Evento(
            id = 4, nombre = "Trekking Cerro San Cristóbal", tipo = "Aire libre",
            descripcion = "Subida por el Parque Metropolitano, salimos temprano.",
            ubicacion = "Providencia", referencia = "Plaza Caupolicán",
            etiquetas = listOf("trekking", "cerro"),
            cupo = 15, asistentes = listOf("demo", "juan", "sofia"),
            imagenesRes = listOf(/* R.drawable.xxx */), latitud =-33.43058920209797,
            longitud = -70.6357201938626, // TODO: imágenes
            creador = "sofia",
        ),
        Evento(
            id = 5, nombre = "Estudio para certamen", tipo = "Estudio",
            descripcion = "Repaso de programación orientada a objetos y Kotlin.",
            ubicacion = "Santiago Centro", referencia = "Biblioteca Nacional",
            etiquetas = listOf("estudio", "kotlin"),
            cupo = 6, asistentes = listOf("demo", "luis"),
            imagenesRes = listOf(/* R.drawable.xxx */), latitud = -33.44190000767134,
            longitud = -70.64558334012956, // TODO: imágenes
            creador = "luis",
        ),
        Evento(
            id = 6, nombre = "Tocata indie", tipo = "Música",
            descripcion = "Bandas locales, entrada liberada.",
            ubicacion = "Barrio Italia", referencia = "Metro Santa Isabel",
            etiquetas = listOf("musica", "tocata"),
            cupo = 40, asistentes = listOf("demo", "nicolas", "camila", "felipe"),
            imagenesRes = listOf(/* R.drawable.xxx */), latitud = -33.44596797815986,
            longitud = -70.6256457717317, // TODO: imágenes
            creador = "nicolas",
        ),
        Evento(
            id = 7, nombre = "Completada", tipo = "Comida",
            descripcion = "Cada uno trae algo para compartir.",
            ubicacion = "La Florida", referencia = "Mall Plaza Vespucio",
            etiquetas = listOf("comida", "completos"),
            cupo = 12, asistentes = listOf("demo", "diego"),
            imagenesRes = listOf(/* R.drawable.xxx */), latitud = -33.51456411781995,
            longitud = -70.59548656291403, // TODO: imágenes
            creador = "diego",
        )
    )

    private fun mensajesIniciales() = mapOf(
        1 to listOf(
            Mensaje("juan", "¿Nos juntamos a las 18:00 en la salida del metro?"),
            Mensaje("sofia", "Dale, llevo dos pases remotos"),
            Mensaje("nicolas", "Voy un poco tarde, me esperan")
        ),
        4 to listOf(
            Mensaje("sofia", "Salimos 8:30 desde Plaza Caupolicán"),
            Mensaje("juan", "¿Hay que llevar algo?"),
            Mensaje("sofia", "Agua y bloqueador")
        ),
        5 to listOf(Mensaje("luis", "Traigan las guías del ramo")),
        6 to listOf(Mensaje("nicolas", "Abren puertas a las 21:00")),
        7 to listOf(Mensaje("diego", "Yo llevo el pan"))
    )


    suspend fun cargarEventosRemotos(): Result<List<EventoDto>> {
        return try {
            val response = XanoRetrofit.eventoApi.obtenerEventos()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener eventos: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publicarEventoRemoto(nuevoEvento: EventoDto): Result<EventoDto> {
        return try {
            val response = XanoRetrofit.eventoApi.crearEvento(nuevoEvento)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear evento: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarEventoRemoto(id: Int): Result<Unit> {
        return try {
            val response = XanoRetrofit.eventoApi.eliminarEvento(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("No se pudo eliminar: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Agrega estos métodos dentro de EventoRepository.kt

    suspend fun obtenerEventoPorIdRemoto(id: Int): Result<EventoDto> {
        return try {
            val response = XanoRetrofit.eventoApi.obtenerEventoPorId(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Evento no encontrado (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarEventoRemoto(id: Int, evento: EventoDto): Result<EventoDto> {
        return try {
            val response = XanoRetrofit.eventoApi.actualizarEvento(id, evento)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar evento (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

fun EventoDto.toDomain(): Evento {
    return Evento(
        id = this.id,
        nombre = this.titulo,
        tipo = this.categoria,
        descripcion = this.descripcion,
        ubicacion = this.ubicacion,
        cupo = this.cupoMaximo,
        latitud = this.latitud ?: -33.5227, // Coordenada por defecto si viene null
        longitud = this.longitud ?: -70.5983,
        cupoMaximo = this.cupoMaximo,
        imagenUrl = this.imagenUrl.orEmpty(),
        creadorId = this.creadorId,
        titulo = this.titulo,
        fecha = this.fecha,
        hora = this.hora,
        categoria = this.categoria
    )
}