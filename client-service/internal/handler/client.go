// Package handler expone la API REST de clientes sobre Gin.
package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"

	"hospital/client-service/internal/model"
)

// Server agrupa las dependencias de los handlers. Equivale a inyectar el
// repositorio por constructor en los servicios Spring.
type Server struct{ db *gorm.DB }

// New crea el servidor con la conexión a la base ya abierta.
func New(db *gorm.DB) *Server { return &Server{db: db} }

// Run registra las rutas y se queda escuchando en la dirección indicada.
//
// Las rutas son las mismas que expone el gateway: lo que aquí es /api/clients,
// el usuario lo ve en http://localhost:8080/api/clients.
func (s *Server) Run(address string) {
	r := gin.Default()

	// Imita el endpoint de Spring Boot Actuator. No es decorativo: es el que
	// consulta el healthcheck de este contenedor en compose.yaml, para que el
	// gateway no arranque antes de que este servicio esté listo.
	r.GET("/actuator/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "UP"})
	})

	r.GET("/api/clients", s.list)
	r.GET("/api/clients/:id", s.get)
	r.POST("/api/clients", s.create)

	_ = r.Run(address)
}

// list devuelve todos los clientes. GET /api/clients
func (s *Server) list(c *gin.Context) {
	var clients []model.Client
	s.db.Find(&clients)
	c.JSON(http.StatusOK, clients)
}

// get devuelve un cliente por id. GET /api/clients/:id — 404 si no existe.
func (s *Server) get(c *gin.Context) {
	var client model.Client
	if err := s.db.First(&client, c.Param("id")).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"message": "client not found"})
		return
	}
	c.JSON(http.StatusOK, client)
}

// create registra un cliente nuevo. POST /api/clients — 201 con el id asignado.
//
// ShouldBindJSON aplica las etiquetas `binding` del modelo: si falta el nombre,
// se responde 400 sin llegar a tocar la base de datos.
func (s *Server) create(c *gin.Context) {
	var client model.Client
	if err := c.ShouldBindJSON(&client); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "name is required"})
		return
	}
	s.db.Create(&client)
	c.JSON(http.StatusCreated, client)
}
