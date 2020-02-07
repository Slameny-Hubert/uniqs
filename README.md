To start:
* Put the paths to the IMDB files to application.conf paths section
* Change the port if necessary (http.port)
* Run HttpService (it requires about 10G of memory, so use --Xmx10G)
* Wait while the caches are loading (about 10 minutes)

The service provides next interfaces:
1. Search by movie title (prime or original)
/api/movie/<Movie name>
Ex:
    http://localhost:9000/api/movie/War%20and%20Peace
    http://localhost:9000/api/movie/Freaks

2. Search by actor name
/api/name/<Actor name>
Ex:
    http://localhost:9000/api/name/Michael%20Ackerman
    http://localhost:9000/api/name/Brigitte%20Bardot

3. Top movies by genre
/api/top/<genre name>?qnt=<positive number>&off=<non negative number>
qnt - quantity of movies to show
off - offset from the first position
Ex:
    http://localhost:9000/api/top/drama?qnt=10&off=0
    http://localhost:9000/api/top/short?qnt=10&off=10

4. Find the coincidence.
/api/together?name=<actor name>[&name=<actor name>...]
Ex:
    http://localhost:9000/api/together?name=Charles%20Kayser&name=John%20Ott&name=William%20K.L.%20Dickson
    http://localhost:9000/api/together?name=Brigitte%20Bardot

All string parameters are case insensitive