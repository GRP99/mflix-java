package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Objects;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {
    private final MongoCollection<User> usersCollection;
    //DO> Ticket: User Management - do the necessary changes so that the sessions' collection returns a Session object
    private final MongoCollection<Session> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //DO> Ticket: User Management - implement the necessary changes so that the sessions' collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        //DO > Ticket: Durable Writes -  you might want to use a more durable write concern here!
        //DO > Ticket: Handling Errors - make sure to only add new users and not users that already exist.
        User check = getUser(user.getEmail());

        if (Objects.equals(check.getEmail(), user.getEmail())) {
            throw new IncorrectDaoOperation("User already exists.");
        }
        usersCollection.withWriteConcern(WriteConcern.MAJORITY).insertOne(user);
        return true;
    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        Session session = new Session();
        session.setUserId(userId);
        session.setJwt(jwt);

        if (sessionsCollection.find(new Document("user_id", userId)).iterator().hasNext()) {
            deleteUserSessions(userId);
        }

        sessionsCollection.insertOne(session);
        return true;
    }

    /**
     * Returns the User object matching the email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        //DO> Ticket: User Management - implement the query that returns the first User object.
        return usersCollection.find(new Document("email", email)).first();
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        //DO> Ticket: User Management - implement the method that returns Sessions for a given userId
        return sessionsCollection.find(new Document("user_id", userId)).first();
    }

    public boolean deleteUserSessions(String userId) {
        //DO> Ticket: User Management - implement to delete user sessions method
        sessionsCollection.findOneAndDelete(new Document("user_id", userId));
        return true;
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
        deleteUserSessions(email);
        //DO> Ticket: User Management - implement the delete user method
        usersCollection.findOneAndDelete(new Document("email", email));
        //DO > Ticket: Handling Errors - make this method more robust by handling potential exceptions.
        return true;
    }

    /**
     * Updates the preferences of a user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        //DO > Ticket: Handling Errors - make this method more robust by handling potential exceptions when updating an entry.
        if (userPreferences == null || userPreferences.isEmpty()) {
            throw new IncorrectDaoOperation("Preferences can not be empty or null.", new Throwable());
        }
        //DO> Ticket: User Preferences - implement the method that allows for user preferences to be updated.
        Document document_preferences = new Document();
        for (String key : userPreferences.keySet()) {
            document_preferences.put(key, userPreferences.get(key).toString());
        }

        usersCollection.updateOne(eq("email", email), set("preferences", document_preferences), new UpdateOptions().upsert(true));

        return true;
    }
}