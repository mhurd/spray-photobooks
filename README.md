spray-photobooks
================

Toying with a re-write of the photobooks web-site in Spray as a single-page web-application

Requires a MongoDB installation:

DB: 'photobooks'
Collections: 'books'

User: 'photobooks'
Pass: 'whateveryouwant'

requires JVM params:

-Duser.timezone=GMT
-Dmongodb.servers="localhost:27017" 
-Dmongodb.database="photobooks" 
-Dmongodb.username="photobooks" 
-Dmongodb.password="whateveryouwant"

requires program parameters <amazon access key> <amazon secret key> <amazon associate tag>

