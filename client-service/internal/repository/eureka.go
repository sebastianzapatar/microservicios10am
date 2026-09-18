package repository

import (
	"bytes"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"
)

const (
	heartbeatInterval = 25 * time.Second
	retryInterval     = 5 * time.Second
)

var eurekaHTTP = &http.Client{Timeout: 5 * time.Second}

// RegisterWithEureka usa la API HTTP nativa de Eureka para que este servicio Go
// participe del descubrimiento igual que los servicios Spring Boot. Mantiene el
// registro vivo con heartbeats y vuelve a registrarse si Eureka deja de conocer
// la instancia (por ejemplo, tras reiniciar el servidor de registro).
func RegisterWithEureka(baseURL, serviceName string, port int) {
	if baseURL == "" {
		baseURL = "http://eureka-server:8761/eureka"
	}
	baseURL = strings.TrimSuffix(baseURL, "/")

	// El DNS interno de Docker resuelve el nombre del servicio desde cualquier contenedor.
	host := serviceName
	app := strings.ToUpper(serviceName)
	instanceID := fmt.Sprintf("%s:%d", host, port)
	appURL := baseURL + "/apps/" + app
	instanceURL := appURL + "/" + instanceID
	body := fmt.Sprintf(`<instance><instanceId>%s</instanceId><hostName>%s</hostName><app>%s</app><ipAddr>%s</ipAddr><vipAddress>%s</vipAddress><secureVipAddress>%s</secureVipAddress><status>UP</status><port enabled="true">%d</port><dataCenterInfo class="com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo"><name>MyOwn</name></dataCenterInfo></instance>`,
		instanceID, host, app, host, app, app, port)

	registerUntilSuccess(appURL, body, instanceID)

	ticker := time.NewTicker(heartbeatInterval)
	defer ticker.Stop()
	for range ticker.C {
		if sendHeartbeat(instanceURL) {
			continue
		}
		// Eureka respondió 404 (o no respondió): la instancia ya no está en el
		// registro, así que hay que anunciarse otra vez.
		log.Printf("eureka: heartbeat rechazado, reintentando registro")
		registerUntilSuccess(appURL, body, instanceID)
	}
}

func registerUntilSuccess(appURL, body, instanceID string) {
	for !register(appURL, body) {
		time.Sleep(retryInterval)
	}
	log.Printf("eureka: registrado como %s", instanceID)
}

func register(appURL, body string) bool {
	req, err := http.NewRequest(http.MethodPost, appURL, bytes.NewBufferString(body))
	if err != nil {
		log.Printf("eureka: no se pudo construir la solicitud de registro: %v", err)
		return false
	}
	req.Header.Set("Content-Type", "application/xml")
	resp, err := eurekaHTTP.Do(req)
	if err != nil {
		log.Printf("eureka: esperando al servidor de registro: %v", err)
		return false
	}
	defer resp.Body.Close()
	return resp.StatusCode == http.StatusNoContent || resp.StatusCode == http.StatusOK
}

func sendHeartbeat(instanceURL string) bool {
	req, err := http.NewRequest(http.MethodPut, instanceURL, nil)
	if err != nil {
		return false
	}
	resp, err := eurekaHTTP.Do(req)
	if err != nil {
		return false
	}
	defer resp.Body.Close()
	return resp.StatusCode == http.StatusOK
}
