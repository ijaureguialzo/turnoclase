help:
	@echo Opciones:
	@echo start / stop
	@echo workspace
	@echo build / clean
	@echo serve

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

serve:
	@cd jekyll && bundle exec jekyll serve
