package com.kolo.kolo_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KoloBackendApplication

fun main(args: Array<String>) {
	runApplication<KoloBackendApplication>(*args)
}
