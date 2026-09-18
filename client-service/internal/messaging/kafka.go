// Package messaging publica los eventos de dominio del servicio de pacientes.
package messaging

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/IBM/sarama"
)

const (
	topic           = "hospital.events"
	connectAttempts = 20
	retryDelay      = 2 * time.Second
)

// Publisher conserva un productor seguro para uso concurrente por los handlers.
type Publisher struct{ producer sarama.SyncProducer }

// DomainEvent coincide con el contrato JSON usado por los productores Java.
type DomainEvent struct {
	EventID     string         `json:"eventId"`
	EventType   string         `json:"eventType"`
	AggregateID string         `json:"aggregateId"`
	OccurredAt  time.Time      `json:"occurredAt"`
	Payload     map[string]any `json:"payload"`
}

// Open conecta con Kafka y reintenta para tolerar reinicios del broker.
func Open(bootstrapServers string) (*Publisher, error) {
	if bootstrapServers == "" {
		bootstrapServers = "kafka:9092"
	}

	config := sarama.NewConfig()
	config.ClientID = "client-service"
	config.Version = sarama.V3_6_0_0
	config.Net.MaxOpenRequests = 1
	config.Producer.Idempotent = true
	config.Producer.RequiredAcks = sarama.WaitForAll
	config.Producer.Retry.Max = 5
	config.Producer.Return.Successes = true

	var producer sarama.SyncProducer
	var err error
	for range connectAttempts {
		producer, err = sarama.NewSyncProducer(strings.Split(bootstrapServers, ","), config)
		if err == nil {
			return &Publisher{producer: producer}, nil
		}
		time.Sleep(retryDelay)
	}
	return nil, fmt.Errorf("kafka no respondió tras %d intentos: %w", connectAttempts, err)
}

// PublishClientCreated publica con clave estable para mantener orden por paciente.
func (p *Publisher) PublishClientCreated(id uint, name string) error {
	key := fmt.Sprintf("client:%d", id)
	event := DomainEvent{
		EventID:     newEventID(),
		EventType:   "client.created",
		AggregateID: key,
		OccurredAt:  time.Now().UTC(),
		Payload:     map[string]any{"id": id, "name": name},
	}
	value, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("serializar evento: %w", err)
	}
	_, _, err = p.producer.SendMessage(&sarama.ProducerMessage{
		Topic: topic,
		Key:   sarama.StringEncoder(key),
		Value: sarama.ByteEncoder(value),
	})
	if err != nil {
		return fmt.Errorf("publicar evento %s: %w", event.EventID, err)
	}
	return nil
}

func (p *Publisher) Close() error { return p.producer.Close() }

func newEventID() string {
	bytes := make([]byte, 16)
	if _, err := rand.Read(bytes); err != nil {
		return fmt.Sprintf("client-%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(bytes)
}
