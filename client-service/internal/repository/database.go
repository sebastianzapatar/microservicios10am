package repository

import (
	"fmt"
	"time"

	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

const (
	dbMaxAttempts = 20
	dbRetryDelay  = 2 * time.Second
)

// Open abre la conexión con MySQL, reintentando hasta que la base acepte clientes.
//
// El reintento no sobra aunque compose.yaml ya espere a que MySQL esté "healthy":
// si el contenedor de MySQL se reinicia con el sistema en marcha, este servicio
// se recupera solo en vez de morir en el arranque.
//
// El DSN llega desde la variable de entorno MYSQL_DSN (ver cmd/server/main.go).
func Open(dsn string) (*gorm.DB, error) {
	var db *gorm.DB
	var err error

	for range dbMaxAttempts {
		db, err = gorm.Open(mysql.Open(dsn), &gorm.Config{})
		if err == nil {
			return db, nil
		}
		time.Sleep(dbRetryDelay)
	}

	// %w envuelve el error original para no perder la causa real del fallo.
	return nil, fmt.Errorf("mysql no respondió tras %d intentos: %w", dbMaxAttempts, err)
}
