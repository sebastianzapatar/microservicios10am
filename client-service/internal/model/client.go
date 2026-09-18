// Package model contiene las estructuras de datos del dominio de clientes.
package model

// Client es un paciente del hospital.
//
// Las etiquetas de cada campo cumplen tres papeles distintos a la vez:
//
//	json    → cómo se llama el campo al entrar y salir en la API REST
//	gorm    → cómo se mapea a la tabla de MySQL
//	binding → qué valida Gin en el cuerpo de la petición
//
// Es el equivalente Go de lo que en los servicios Java hacen @Entity y @Column.
type Client struct {
	// ID lo genera MySQL con AUTO_INCREMENT; no se envía al crear un cliente.
	ID uint `json:"id" gorm:"primaryKey"`

	// Name es obligatorio: sin él, Gin rechaza la petición con 400.
	Name string `json:"name" gorm:"not null" binding:"required"`
}
