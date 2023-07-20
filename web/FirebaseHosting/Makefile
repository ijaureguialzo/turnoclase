help: _header
	${info }
	@echo Opciones:
	@echo --------------------
	@echo build
	@echo workspace
	@echo deploy
	@echo update / serve
	@echo clean
	@echo --------------------

_header:
	@echo --------------------
	@echo TurnoClase - Website
	@echo --------------------

build:
	@docker compose build --pull

workspace:
	@docker compose run --rm node /bin/bash

deploy:
	@cd jekyll && bundle exec jekyll build
	@cd ..
	@docker compose run --rm --service-ports node /bin/sh -c 'firebase login && firebase use turnoclase-eu && firebase deploy --only hosting'

update:
	@cd jekyll && bundle update

serve:
	@cd jekyll && bundle exec jekyll serve

clean:
	@docker compose down -v --remove-orphans
