package com.nexora.user.oauth;

import com.nexora.user.domain.UserAccount;
import java.time.Instant;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/** Bind only an unoccupied provider field; preserve simultaneous profile/other-provider changes. */
@Component
class OAuthAccountLinker {
    private final MongoOperations mongo;

    OAuthAccountLinker(MongoOperations mongo) {
        this.mongo = mongo;
    }

    UserAccount link(UserAccount expected, OAuthIdentity identity) {
        String field = identity.provider() == OAuthProvider.GOOGLE ? "googleSubject" : "githubSubject";
        Query query = Query.query(Criteria.where("_id").is(expected.getId())
                .and("passwordHash").is(expected.getPasswordHash()).and(field).is(null));
        UserAccount linked = mongo.findAndModify(query,
                new Update().set(field, identity.subject()).set("updatedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true), UserAccount.class);
        if (linked == null) {
            throw new OAuthFlowException(409, "This account changed. Please start again.");
        }
        return linked;
    }
}
