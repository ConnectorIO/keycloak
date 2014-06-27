package org.keycloak.model.test;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.OAuthClientManager;
import org.keycloak.services.managers.RealmManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdapterTest extends AbstractModelTest {
    private RealmModel realmModel;

    @Test
    public void test1CreateRealm() throws Exception {
        realmModel = realmManager.createRealm("JUGGLER");
        realmModel.setAccessCodeLifespan(100);
        realmModel.setAccessCodeLifespanUserAction(600);
        realmModel.setEnabled(true);
        realmModel.setName("JUGGLER");
        realmModel.setPrivateKeyPem("0234234");
        realmModel.setPublicKeyPem("0234234");
        realmModel.setAccessTokenLifespan(1000);
        realmModel.setUpdateProfileOnInitialSocialLogin(true);
        realmModel.addDefaultRole("foo");

        realmModel = realmManager.getRealm(realmModel.getId());
        assertNotNull(realmModel);
        Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
        Assert.assertEquals(600, realmModel.getAccessCodeLifespanUserAction());
        Assert.assertEquals(realmModel.getAccessTokenLifespan(), 1000);
        Assert.assertEquals(realmModel.isEnabled(), true);
        Assert.assertEquals(realmModel.getName(), "JUGGLER");
        Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
        Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
        Assert.assertEquals(realmModel.isUpdateProfileOnInitialSocialLogin(), true);
        Assert.assertEquals(1, realmModel.getDefaultRoles().size());
        Assert.assertEquals("foo", realmModel.getDefaultRoles().get(0));
    }

    @Test
    public void testRealmListing() throws Exception {
        realmModel = realmManager.createRealm("JUGGLER");
        realmModel.setAccessCodeLifespan(100);
        realmModel.setAccessCodeLifespanUserAction(600);
        realmModel.setEnabled(true);
        realmModel.setName("JUGGLER");
        realmModel.setPrivateKeyPem("0234234");
        realmModel.setPublicKeyPem("0234234");
        realmModel.setAccessTokenLifespan(1000);
        realmModel.setUpdateProfileOnInitialSocialLogin(true);
        realmModel.addDefaultRole("foo");

        realmModel = realmManager.getRealm(realmModel.getId());
        assertNotNull(realmModel);
        Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
        Assert.assertEquals(600, realmModel.getAccessCodeLifespanUserAction());
        Assert.assertEquals(realmModel.getAccessTokenLifespan(), 1000);
        Assert.assertEquals(realmModel.isEnabled(), true);
        Assert.assertEquals(realmModel.getName(), "JUGGLER");
        Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
        Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
        Assert.assertEquals(realmModel.isUpdateProfileOnInitialSocialLogin(), true);
        Assert.assertEquals(1, realmModel.getDefaultRoles().size());
        Assert.assertEquals("foo", realmModel.getDefaultRoles().get(0));

        realmModel.getId();

        commit();
        List<RealmModel> realms = session.getRealms();
        Assert.assertEquals(realms.size(), 2);
    }


    @Test
    public void test2RequiredCredential() throws Exception {
        test1CreateRealm();
        realmModel.addRequiredCredential(CredentialRepresentation.PASSWORD);
        List<RequiredCredentialModel> storedCreds = realmModel.getRequiredCredentials();
        Assert.assertEquals(1, storedCreds.size());

        Set<String> creds = new HashSet<String>();
        creds.add(CredentialRepresentation.PASSWORD);
        creds.add(CredentialRepresentation.TOTP);
        realmModel.updateRequiredCredentials(creds);
        storedCreds = realmModel.getRequiredCredentials();
        Assert.assertEquals(2, storedCreds.size());
        boolean totp = false;
        boolean password = false;
        for (RequiredCredentialModel cred : storedCreds) {
            Assert.assertTrue(cred.isInput());
            if (cred.getType().equals(CredentialRepresentation.PASSWORD)) {
                password = true;
                Assert.assertTrue(cred.isSecret());
            } else if (cred.getType().equals(CredentialRepresentation.TOTP)) {
                totp = true;
                Assert.assertFalse(cred.isSecret());
            }
        }
        Assert.assertTrue(totp);
        Assert.assertTrue(password);
    }

    @Test
    public void testCredentialValidation() throws Exception {
        test1CreateRealm();
        UserModel user = realmModel.addUser("bburke");
        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("geheim");
        user.updateCredential(cred);
        Assert.assertTrue(realmModel.validatePassword(user, "geheim"));
    }

    @Test
    public void testOAuthClient() throws Exception {
        test1CreateRealm();

        OAuthClientModel oauth = new OAuthClientManager(realmModel).create("oauth-client");
        oauth = realmModel.getOAuthClient("oauth-client");
    }

    @Test
    public void testDeleteUser() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");
        user.setAttribute("attr1", "val1");
        user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        RoleModel testRole = realmModel.addRole("test");
        user.grantRole(testRole);

        ApplicationModel app = realmModel.addApplication("test-app");
        RoleModel appRole = app.addRole("test");
        user.grantRole(appRole);

        SocialLinkModel socialLink = new SocialLinkModel("google", "google1", user.getLoginName());
        realmModel.addSocialLink(user, socialLink);

        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("password");
        user.updateCredential(cred);

        commit();

        realmModel = session.getRealm("JUGGLER");
        Assert.assertTrue(realmModel.removeUser("bburke"));
        Assert.assertFalse(realmModel.removeUser("bburke"));
        assertNull(realmModel.getUser("bburke"));
    }

    @Test
    public void testRemoveApplication() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");

        OAuthClientModel client = realmModel.addOAuthClient("client");

        ApplicationModel app = realmModel.addApplication("test-app");

        RoleModel appRole = app.addRole("test");
        user.grantRole(appRole);
        client.addScopeMapping(appRole);

        RoleModel realmRole = realmModel.addRole("test");
        app.addScopeMapping(realmRole);

        Assert.assertTrue(realmModel.removeApplication(app.getId()));
        Assert.assertFalse(realmModel.removeApplication(app.getId()));
        assertNull(realmModel.getApplicationById(app.getId()));
    }


    @Test
    public void testRemoveRealm() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");

        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("password");
        user.updateCredential(cred);

        OAuthClientModel client = realmModel.addOAuthClient("client");

        ApplicationModel app = realmModel.addApplication("test-app");

        RoleModel appRole = app.addRole("test");
        user.grantRole(appRole);
        client.addScopeMapping(appRole);

        RoleModel realmRole = realmModel.addRole("test");
        RoleModel realmRole2 = realmModel.addRole("test2");
        realmRole.addCompositeRole(realmRole2);
        realmRole.addCompositeRole(appRole);

        app.addScopeMapping(realmRole);

        commit();
        realmModel = session.getRealm("JUGGLER");

        Assert.assertTrue(realmManager.removeRealm(realmModel));
        Assert.assertFalse(realmManager.removeRealm(realmModel));
        assertNull(realmManager.getRealm(realmModel.getId()));
    }


    @Test
    public void testRemoveRole() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");

        OAuthClientModel client = realmModel.addOAuthClient("client");

        ApplicationModel app = realmModel.addApplication("test-app");

        RoleModel appRole = app.addRole("test");
        user.grantRole(appRole);
        client.addScopeMapping(appRole);

        RoleModel realmRole = realmModel.addRole("test");
        app.addScopeMapping(realmRole);

        commit();
        realmModel = session.getRealm("JUGGLER");
        app = realmModel.getApplicationByName("test-app");

        Assert.assertTrue(realmModel.removeRoleById(realmRole.getId()));
        Assert.assertFalse(realmModel.removeRoleById(realmRole.getId()));
        assertNull(realmModel.getRole(realmRole.getName()));

        Assert.assertTrue(realmModel.removeRoleById(appRole.getId()));
        Assert.assertFalse(realmModel.removeRoleById(appRole.getId()));
        assertNull(app.getRole(appRole.getName()));
    }

    @Test
    public void testUserSearch() throws Exception {
        test1CreateRealm();
        {
            UserModel user = realmModel.addUser("bburke");
            user.setLastName("Burke");
            user.setFirstName("Bill");
            user.setEmail("bburke@redhat.com");

            UserModel user2 = realmModel.addUser("doublefirst");
            user2.setFirstName("Knut Ole");
            user2.setLastName("Alver");
            user2.setEmail("knut@redhat.com");

            UserModel user3 = realmModel.addUser("doublelast");
            user3.setFirstName("Ole");
            user3.setLastName("Alver Veland");
            user3.setEmail("knut2@redhat.com");
        }

        RealmManager adapter = realmManager;

        {
            List<UserModel> userModels = adapter.searchUsers("total junk query", realmModel);
            Assert.assertEquals(userModels.size(), 0);
        }

        {
            List<UserModel> userModels = adapter.searchUsers("Bill Burke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("bill burk", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            ArrayList<String> users = new ArrayList<String>();
            for (UserModel u : adapter.searchUsers("ole alver", realmModel)) {
                users.add(u.getLoginName());
            }
            String[] usernames = users.toArray(new String[users.size()]);
            Arrays.sort(usernames);
            Assert.assertArrayEquals(new String[]{"doublefirst", "doublelast"}, usernames);
        }

        {
            List<UserModel> userModels = adapter.searchUsers("bburke@redhat.com", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("rke@redhat.com", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("bburke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("BurK", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("Burke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            UserModel user = realmModel.addUser("mburke");
            user.setLastName("Burke");
            user.setFirstName("Monica");
            user.setEmail("mburke@redhat.com");
        }

        {
            UserModel user = realmModel.addUser("thor");
            user.setLastName("Thorgersen");
            user.setFirstName("Stian");
            user.setEmail("thor@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("Monica Burke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Monica");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "mburke@redhat.com");
        }


        {
            List<UserModel> userModels = adapter.searchUsers("mburke@redhat.com", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Monica");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "mburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("mburke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Monica");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "mburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("Burke", realmModel);
            Assert.assertEquals(userModels.size(), 2);
            UserModel first = userModels.get(0);
            UserModel second = userModels.get(1);
            if (!first.getEmail().equals("bburke@redhat.com") && !second.getEmail().equals("bburke@redhat.com")) {
                Assert.fail();
            }
            if (!first.getEmail().equals("mburke@redhat.com") && !second.getEmail().equals("mburke@redhat.com")) {
                Assert.fail();
            }
        }

        RealmModel otherRealm = adapter.createRealm("other");
        otherRealm.addUser("bburke");

        Assert.assertEquals(1, otherRealm.getUsers().size());
        Assert.assertEquals(1, otherRealm.searchForUser("bu").size());
    }


    @Test
    public void testRoles() throws Exception {
        test1CreateRealm();
        realmModel.addRole("admin");
        realmModel.addRole("user");
        Set<RoleModel> roles = realmModel.getRoles();
        Assert.assertEquals(3, roles.size());
        UserModel user = realmModel.addUser("bburke");
        RoleModel realmUserRole = realmModel.getRole("user");
        user.grantRole(realmUserRole);
        Assert.assertTrue(user.hasRole(realmUserRole));
        RoleModel found = realmModel.getRoleById(realmUserRole.getId());
        assertNotNull(found);
        assertRolesEquals(found, realmUserRole);

        // Test app roles
        ApplicationModel application = realmModel.addApplication("app1");
        application.addRole("user");
        application.addRole("bar");
        Set<RoleModel> appRoles = application.getRoles();
        Assert.assertEquals(2, appRoles.size());
        RoleModel appBarRole = application.getRole("bar");
        assertNotNull(appBarRole);

        found = realmModel.getRoleById(appBarRole.getId());
        assertNotNull(found);
        assertRolesEquals(found, appBarRole);

        user.grantRole(appBarRole);
        user.grantRole(application.getRole("user"));

        roles = user.getRealmRoleMappings();
        Assert.assertEquals(roles.size(), 2);
        assertRolesContains(realmUserRole, roles);
        Assert.assertTrue(user.hasRole(realmUserRole));
        // Role "foo" is default realm role
        Assert.assertTrue(user.hasRole(realmModel.getRole("foo")));

        roles = user.getApplicationRoleMappings(application);
        Assert.assertEquals(roles.size(), 2);
        assertRolesContains(application.getRole("user"), roles);
        assertRolesContains(appBarRole, roles);
        Assert.assertTrue(user.hasRole(appBarRole));

        // Test that application role 'user' don't clash with realm role 'user'
        Assert.assertNotEquals(realmModel.getRole("user").getId(), application.getRole("user").getId());

        Assert.assertEquals(6, user.getRoleMappings().size());

        // Revoke some roles
        user.deleteRoleMapping(realmModel.getRole("foo"));
        user.deleteRoleMapping(appBarRole);
        roles = user.getRoleMappings();
        Assert.assertEquals(4, roles.size());
        assertRolesContains(realmUserRole, roles);
        assertRolesContains(application.getRole("user"), roles);
        Assert.assertFalse(user.hasRole(appBarRole));
    }

    @Test
    public void testScopes() throws Exception {
        test1CreateRealm();
        RoleModel realmRole = realmModel.addRole("realm");

        ApplicationModel app1 = realmModel.addApplication("app1");
        RoleModel appRole = app1.addRole("app");

        ApplicationModel app2 = realmModel.addApplication("app2");
        app2.addScopeMapping(realmRole);
        app2.addScopeMapping(appRole);

        OAuthClientModel client = realmModel.addOAuthClient("client");
        client.addScopeMapping(realmRole);
        client.addScopeMapping(appRole);

        commit();

        realmModel = session.getRealmByName("JUGGLER");
        app1 = realmModel.getApplicationByName("app1");
        app2 = realmModel.getApplicationByName("app2");
        client = realmModel.getOAuthClient("client");

        Set<RoleModel> scopeMappings = app2.getScopeMappings();
        Assert.assertEquals(2, scopeMappings.size());
        Assert.assertTrue(scopeMappings.contains(realmModel.getRole("realm")));
        Assert.assertTrue(scopeMappings.contains(app1.getRole("app")));

        scopeMappings = client.getScopeMappings();
        Assert.assertEquals(2, scopeMappings.size());
        Assert.assertTrue(scopeMappings.contains(realmModel.getRole("realm")));
        Assert.assertTrue(scopeMappings.contains(app1.getRole("app")));
    }

    @Test
    public void testRealmNameCollisions() throws Exception {
        test1CreateRealm();

        commit();

        // Try to create realm with duplicate name
        try {
            test1CreateRealm();
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }
        commit(true);

        // Try to rename realm to duplicate name
        realmManager.createRealm("JUGGLER2");
        commit();
        try {
            realmManager.getRealmByName("JUGGLER2").setName("JUGGLER");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }

        resetSession();
    }

    @Test
    public void testAppNameCollisions() throws Exception {
        realmManager.createRealm("JUGGLER1").addApplication("app1");
        realmManager.createRealm("JUGGLER2").addApplication("app1");

        commit();

        // Try to create app with duplicate name
        try {
            realmManager.getRealmByName("JUGGLER1").addApplication("app1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }
        commit(true);

        // Ty to rename app to duplicate name
        realmManager.getRealmByName("JUGGLER1").addApplication("app2");
        commit();
        try {
            realmManager.getRealmByName("JUGGLER1").getApplicationByName("app2").setName("app1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }

        resetSession();
    }

    @Test
    public void testClientNameCollisions() throws Exception {
        realmManager.createRealm("JUGGLER1").addOAuthClient("client1");
        realmManager.createRealm("JUGGLER2").addOAuthClient("client1");

        commit();

        // Try to create app with duplicate name
        try {
            realmManager.getRealmByName("JUGGLER1").addOAuthClient("client1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }
        commit(true);

        // Ty to rename app to duplicate name
        realmManager.getRealmByName("JUGGLER1").addOAuthClient("client2");
        commit();
        try {
            realmManager.getRealmByName("JUGGLER1").getOAuthClient("client2").setClientId("client1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }

        resetSession();
    }

    @Test
    public void testUsernameCollisions() throws Exception {
        realmManager.createRealm("JUGGLER1").addUser("user1");
        realmManager.createRealm("JUGGLER2").addUser("user1");
        commit();

        // Try to create user with duplicate login name
        try {
            realmManager.getRealmByName("JUGGLER1").addUser("user1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }
        commit(true);

        // Ty to rename user to duplicate login name
        realmManager.getRealmByName("JUGGLER1").addUser("user2");
        commit();
        try {
            realmManager.getRealmByName("JUGGLER1").getUser("user2").setLoginName("user1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }

        resetSession();
    }

    @Test
    public void testEmailCollisions() throws Exception {
        realmManager.createRealm("JUGGLER1").addUser("user1").setEmail("email@example.com");
        realmManager.createRealm("JUGGLER2").addUser("user1").setEmail("email@example.com");
        commit();

        // Try to create user with duplicate email
        try {
            realmManager.getRealmByName("JUGGLER1").addUser("user2").setEmail("email@example.com");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }

        resetSession();

        // Ty to rename user to duplicate email
        realmManager.getRealmByName("JUGGLER1").addUser("user3").setEmail("email2@example.com");
        commit();
        try {
            realmManager.getRealmByName("JUGGLER1").getUser("user3").setEmail("email@example.com");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }

        resetSession();
    }

    @Test
    public void testAppRoleCollisions() throws Exception {
        realmManager.createRealm("JUGGLER1").addRole("role1");
        realmManager.getRealmByName("JUGGLER1").addApplication("app1").addRole("role1");
        realmManager.getRealmByName("JUGGLER1").addApplication("app2").addRole("role1");

        commit();

        // Try to add role with same name
        try {
            realmManager.getRealmByName("JUGGLER1").getApplicationByName("app1").addRole("role1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }
        commit(true);

        // Ty to rename role to duplicate name
        realmManager.getRealmByName("JUGGLER1").getApplicationByName("app1").addRole("role2");
        commit();
        try {
            realmManager.getRealmByName("JUGGLER1").getApplicationByName("app1").getRole("role2").setName("role1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }

        resetSession();
    }

    @Test
    public void testRealmRoleCollisions() throws Exception {
        realmManager.createRealm("JUGGLER1").addRole("role1");
        realmManager.getRealmByName("JUGGLER1").addApplication("app1").addRole("role1");
        realmManager.getRealmByName("JUGGLER1").addApplication("app2").addRole("role1");

        commit();

        // Try to add role with same name
        try {
            realmManager.getRealmByName("JUGGLER1").addRole("role1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }
        commit(true);

        // Ty to rename role to duplicate name
        realmManager.getRealmByName("JUGGLER1").addRole("role2");
        commit();
        try {
            realmManager.getRealmByName("JUGGLER1").getRole("role2").setName("role1");
            commit();
            Assert.fail("Expected exception");
        } catch (ModelDuplicateException e) {
        }

        resetSession();
    }

    @Test
    public void userSessions() throws InterruptedException {
        realmManager.createRealm("userSessions");
        realmManager.getRealmByName("userSessions").setSsoSessionIdleTimeout(5);

        UserModel user = realmManager.getRealmByName("userSessions").addUser("userSessions1");

        UserSessionModel userSession = realmManager.getRealmByName("userSessions").createUserSession(user, "127.0.0.1");
        commit();

        assertNotNull(realmManager.getRealmByName("userSessions").getUserSession(userSession.getId()));
        commit();

        realmManager.getRealmByName("userSessions").removeUserSession(realmManager.getRealmByName("userSessions").getUserSession(userSession.getId()));
        commit();

        assertNull(realmManager.getRealmByName("userSessions").getUserSession(userSession.getId()));

        userSession = realmManager.getRealmByName("userSessions").createUserSession(user, "127.0.0.1");
        commit();

        realmManager.getRealmByName("userSessions").removeUserSessions(user);
        commit();

        assertNull(realmManager.getRealmByName("userSessions").getUserSession(userSession.getId()));

        realmManager.getRealmByName("userSessions").setSsoSessionIdleTimeout(1);

        userSession = realmManager.getRealmByName("userSessions").createUserSession(user, "127.0.0.1");
        commit();

        Thread.sleep(2000);

        realmManager.getRealmByName("userSessions").removeExpiredUserSessions();
        commit();

        assertNull(realmManager.getRealmByName("userSessions").getUserSession(userSession.getId()));
    }

    @Test
    public void userSessionAssociations() {
        RealmModel realm = realmManager.createRealm("userSessions");
        UserModel user = realm.addUser("userSessions1");
        UserSessionModel userSession = realm.createUserSession(user, "127.0.0.1");

        ApplicationModel app1 = realm.addApplication("app1");
        ApplicationModel app2 = realm.addApplication("app2");
        OAuthClientModel client1 = realm.addOAuthClient("client1");

        Assert.assertEquals(0, userSession.getClientAssociations().size());

        userSession.associateClient(app1);
        userSession.associateClient(client1);

        Assert.assertEquals(2, userSession.getClientAssociations().size());
        Assert.assertTrue(app1.getUserSessions().contains(userSession));
        Assert.assertFalse(app2.getUserSessions().contains(userSession));
        Assert.assertTrue(client1.getUserSessions().contains(userSession));

        commit();

        // Refresh all
        realm = realmManager.getRealm("userSessions");
        userSession = realm.getUserSession(userSession.getId());
        app1 = realm.getApplicationByName("app1");
        client1 = realm.getOAuthClient("client1");

        userSession.removeAssociatedClient(app1);
        Assert.assertEquals(1, userSession.getClientAssociations().size());
        Assert.assertEquals(client1, userSession.getClientAssociations().get(0));
        Assert.assertFalse(app1.getUserSessions().contains(userSession));

        commit();

        // Refresh all
        realm = realmManager.getRealm("userSessions");
        userSession = realm.getUserSession(userSession.getId());
        client1 = realm.getOAuthClient("client1");

        userSession.removeAssociatedClient(client1);
        Assert.assertEquals(0, userSession.getClientAssociations().size());
        Assert.assertFalse(client1.getUserSessions().contains(userSession));
    }

}
