help: _header
	${info }
	@echo Opciones:
	@echo --------------------
	@echo build
	@echo workspace
	@echo deploy
	@echo clean
	@echo --------------------

_header:
	@echo --------------------
	@echo TurnoClase - Backend
	@echo --------------------

build:
	@docker compose build --pull

workspace:
	@docker compose run --rm node /bin/bash

deploy:
	@docker compose run --rm --service-ports node /bin/sh -c 'firebase login && firebase use turnoclase-eu && firebase deploy --only functions'

clean:
	@docker compose down -v --remove-orphans
