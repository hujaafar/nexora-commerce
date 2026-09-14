package com.nexora.user.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nexora.user.domain.Role;
import com.nexora.user.domain.UserAccount;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;

class OAuthAccountLinkerTest {
    @Test
    void bindingIsConditionalAndNeverReplacesRoleOrAnotherProvider() {
        MongoOperations mongo = mock(MongoOperations.class);
        UserAccount user = new UserAccount("Seller", "seller@example.test", "hash", Role.SELLER, OAuthTestTime.NOW);
        ReflectionTestUtils.setField(user, "id", "seller-id");
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(UserAccount.class)))
                .thenReturn(user);
        OAuthIdentity identity = new OAuthIdentity(OAuthProvider.GOOGLE, "subject", user.getEmail(), user.getName());
        assertThat(new OAuthAccountLinker(mongo).link(user, identity)).isSameAs(user);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(query.capture(), update.capture(), any(FindAndModifyOptions.class), eq(UserAccount.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", "seller-id")
                .containsEntry("passwordHash", "hash").containsEntry("googleSubject", null);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class))
                .containsEntry("googleSubject", "subject").doesNotContainKeys("role", "githubSubject", "passwordHash", "name");
    }

    @Test
    void changedAccountOrDuplicateBindingCannotSucceed() {
        MongoOperations mongo = mock(MongoOperations.class);
        OAuthAccountLinker linker = new OAuthAccountLinker(mongo);
        UserAccount user = new UserAccount("Member", "member@example.test", "hash", Role.CLIENT, OAuthTestTime.NOW);
        OAuthIdentity identity = new OAuthIdentity(OAuthProvider.GITHUB, "1234", user.getEmail(), user.getName());
        assertThatThrownBy(() -> linker.link(user, identity)).isInstanceOf(OAuthFlowException.class);
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(UserAccount.class)))
                .thenThrow(new DuplicateKeyException("identity already belongs to another account"));
        assertThatThrownBy(() -> linker.link(user, identity)).isInstanceOf(DuplicateKeyException.class);
    }
}
