help:
	@echo Opciones:
	@echo start / stop
	@echo workspace
	@echo build / clean
	@deploy

start:
	@docker-compose up -d

stop:
	@docker-compose stop

workspace: start
	@docker-compose exec node /bin/bash

build:
	@docker-compose build --pull

clean:
	@docker-compose down

deploy:
	@docker-compose exec node firebase login
	@docker-compose exec node firebase deploy --only functions
