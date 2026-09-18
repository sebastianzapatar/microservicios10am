// client-service: microservicio de pacientes del hospital.
//
// Es el único servicio escrito en Go. Cumple el mismo contrato que sus vecinos
// Java —se registra en Eureka, expone /actuator/health y sirve su API bajo
// /api/clients— pero sin Spring: demuestra que en una arquitectura de
// microservicios el lenguaje de cada servicio es una decisión local, siempre que
// respete los contratos compartidos.
package main

import (
	"fmt"
	"log"
	"os"

	"hospital/client-service/internal/handler"
	"hospital/client-service/internal/model"
	"hospital/client-service/internal/repository"
)

const (
	servicePort = 8083
	serviceName = "client-service"

	// Valor por defecto pensado para Docker Compose: "mysql" es el nombre del
	// servicio en compose.yaml y el DNS interno lo resuelve al contenedor.
	defaultDSN = "hospital:hospital@tcp(mysql:3306)/clientdb?charset=utf8mb4&parseTime=True&loc=Local"
)

func main() {
	// La configuración llega por variables de entorno, no por fichero: es lo que
	// permite usar la misma imagen en Compose, en otra red o en local.
	dsn := os.Getenv("MYSQL_DSN")
	if dsn == "" {
		dsn = defaultDSN
	}

	// 1. Conectar con MySQL (reintentando si aún no está lista).
	db, err := repository.Open(dsn)
	if err != nil {
		log.Fatal(err)
	}

	// 2. Crear o ajustar la tabla de clientes. Equivale al ddl-auto: update de
	//    Hibernate en doctor-service.
	if err = db.AutoMigrate(&model.Client{}); err != nil {
		log.Fatal(err)
	}

	// 3. Anunciarse en Eureka en segundo plano. Va en una goroutine porque el
	//    registro reintenta y luego manda heartbeats para siempre: si se hiciera
	//    aquí en línea, la API nunca llegaría a levantarse.
	go repository.RegisterWithEureka(os.Getenv("EUREKA_URL"), serviceName, servicePort)

	// 4. Servir la API. Esta llamada bloquea y mantiene vivo el proceso.
	handler.New(db).Run(fmt.Sprintf(":%d", servicePort))
}
