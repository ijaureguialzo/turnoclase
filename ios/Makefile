#!make

help: _header
	${info }
	@echo Opciones:
	@echo -------------------------
	@echo profesor / alumno / todas
	@echo -------------------------

_header:
	@echo ----------
	@echo TurnoClase
	@echo ----------

profesor:
	@cd TurnoClaseProfesor/fastlane; bundle exec fastlane ios publicar; cd ../../

alumno:
	@cd TurnoClase/fastlane; bundle exec fastlane ios publicar; cd ../../

todas: profesor alumno
