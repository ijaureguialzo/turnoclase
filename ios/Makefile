#!make

help: _header
	${info }
	@echo Opciones:
	@echo -------------------------
	@echo alumno / profesor / todas
	@echo -------------------------

_header:
	@echo ----------
	@echo TurnoClase
	@echo ----------

alumno:
	@cd TurnoClase/fastlane; bundle exec fastlane ios publicar; cd ../../

profesor:
	@cd TurnoClaseProfesor/fastlane; bundle exec fastlane ios publicar; cd ../../

todas: alumno profesor
