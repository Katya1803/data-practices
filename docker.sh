#!/bin/bash
set -e
cd "$(dirname "$0")"

CMD=$1
SVC=$2

case "$CMD" in

  run)
    # Build images and start everything (use this first time or after code changes)
    docker compose up --build -d
    echo ""
    echo "All services starting. Run './docker.sh logs' to follow logs."
    echo "UI: http://localhost:5173"
    ;;

  start)
    # Start without rebuilding (faster, use when images already built)
    docker compose up -d
    echo ""
    echo "All services started. Run './docker.sh logs' to follow logs."
    echo "UI: http://localhost:5173"
    ;;

  stop)
    # Stop all containers (keep data volumes)
    docker compose down
    ;;

  reset)
    # Stop + delete all data volumes (fresh start, re-restores dataset)
    docker compose down -v
    ;;

  restart)
    # Restart one service or all: ./docker.sh restart springboot
    if [ -n "$SVC" ]; then
      docker compose restart "$SVC"
    else
      docker compose restart
    fi
    ;;

  rebuild)
    # Rebuild + restart one service: ./docker.sh rebuild springboot
    if [ -z "$SVC" ]; then
      echo "Usage: ./docker.sh rebuild <service>"
      echo "Services: springboot | django | react"
      exit 1
    fi
    docker compose up --build -d "$SVC"
    ;;

  logs)
    # Follow logs: ./docker.sh logs springboot
    if [ -n "$SVC" ]; then
      docker compose logs -f "$SVC"
    else
      docker compose logs -f
    fi
    ;;

  ps)
    # Show status of all containers
    docker compose ps
    ;;

  db)
    # Open psql shell in dvdrental database
    docker exec -it dvdrental-postgres psql -U postgres -d dvdrental
    ;;

  redis)
    # Open redis-cli
    docker exec -it dvdrental-redis redis-cli
    ;;

  kafka)
    # List kafka topics
    docker exec dvdrental-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
    ;;

  *)
    echo ""
    echo "Usage: ./docker.sh <command> [service]"
    echo ""
    echo "Commands:"
    echo "  run              Build images and start all services"
    echo "  start            Start without rebuilding"
    echo "  stop             Stop all containers (keep data)"
    echo "  reset            Stop and delete all data (fresh start)"
    echo "  restart [svc]    Restart all or one service"
    echo "  rebuild <svc>    Rebuild and restart one service"
    echo "  logs [svc]       Follow logs (all or one service)"
    echo "  ps               Show container status"
    echo "  db               Open psql shell (dvdrental)"
    echo "  redis            Open redis-cli"
    echo "  kafka            List kafka topics"
    echo ""
    echo "Services: postgres | kafka | redis | springboot | django | react"
    echo ""
    ;;
esac
