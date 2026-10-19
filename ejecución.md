
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # luego cerrar sesión y volver a entrar

-----------------------------------------------

EJECUCION CODIGO

git clone https://github.com/pau091/Plataforma_de_Renta_de_Herramientas.git
cd Plataforma_de_Renta_de_Herramientas
docker compose up -d --build

--------------------------------------------------
MAC 

docker compose up -d --build
until curl -s -o /dev/null http://localhost:8080; do sleep 3; done; open http://localhost:8080

LINUX
docker compose up -d --build
until curl -s -o /dev/null http://localhost:8080; do sleep 3; done; xdg-open http://localhost:8080


Ver si los contenedores están corriendo: docker compose ps
Ver el arranque de la app (salir con Ctrl+C): 	docker compose logs -f tomcat
Apagar la página (conserva los datos):	docker compose down
Volver a encenderla sin recompilar:	docker compose up -d
Apagar y volver a los datos de demostración:	docker compose down -v