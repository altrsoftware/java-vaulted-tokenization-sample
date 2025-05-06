export ALTR_KEY=<ALTR_MANAGEMENT_API_KEY>
export ALTR_SECRET=<ALTR_MANAGEMENT_API_SECRET>
export ALTR_ORG_ID=<ALTR_ORG_ID>
mvn clean package
java -cp target/altr-vaulted-tokenization-jar-with-dependencies.jar com.altr.IntegrationTest