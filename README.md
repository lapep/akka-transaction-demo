# akka-transaction-demo
Money transactions demo API built with Scala, AKKA HTTP and Actors
Sample example application which implements transaction functionality with simple roll back mechanism.

App exposes `/transaction endpoint` on port `3000`

Expected input: POST request of type 'application/json' with a request body:
`{"sender": "1", "receiver": "2", "amount": 10.00}`

Expected output: 200 Accepted with the json body of:
`{"sender":1,"receiver":2,"amount":10.0,"message":"Completed","info":"Success"}`

To run the application: import as SBT project and run `sbt run` or package the app with all the dependecies
by running `sbt assembly` command and execute as a JAR.
In it's in-memory "database" it has two users 1 with a balance of 100, and 2 with a balance of 100 as well.
It can be changed by modifying underlying HashMap in the Main object.
