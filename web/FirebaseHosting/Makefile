help:
	@echo Opciones:
	@echo start / stop
	@echo workspace
	@echo build / clean
	@echo serve
	@echo deploy
	
start:
	@docker-compose up -d

stop:
	@docker-compose stop

workspace: start
	@docker-compose exec node /bin/bash

build:
	@docker-compose build --pull

clean:
	@docker-compose down -v

serve: start
	@cd jekyll && bundle exec jekyll serve

deploy: start
	@cd jekyll && bundle exec jekyll build
	@cd ..
	@docker-compose exec node firebase login
	@docker-compose exec node firebase deploy --only hosting
