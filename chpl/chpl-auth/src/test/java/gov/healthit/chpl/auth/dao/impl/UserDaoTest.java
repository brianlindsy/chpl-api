package gov.healthit.chpl.auth.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.healthit.chpl.auth.dao.UserDAO;
import gov.healthit.chpl.auth.dao.UserPermissionDAO;
import gov.healthit.chpl.auth.dto.UserDTO;
import gov.healthit.chpl.auth.dto.UserPermissionDTO;
import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.auth.permission.UserPermissionRetrievalException;
import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.auth.user.UserCreationException;
import gov.healthit.chpl.auth.user.UserRetrievalException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.auth.CHPLAuthenticationSecurityTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class, DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml")
public class UserDaoTest {

    @Autowired
    private UserDAO dao;
    @Autowired
    private UserPermissionDAO permDao;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private static final String ROLE_ACB = "ROLE_ACB";
    private static JWTAuthenticatedUser authUser;

    @BeforeClass
    public static void setUpClass() throws Exception {
        authUser = new JWTAuthenticatedUser();
        authUser.setFullName("Administrator");
        authUser.setId(-2L);
        authUser.setFriendlyName("Administrator");
        authUser.setSubjectName("admin");
        authUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(authUser);
    }

    @Test(expected = UserRetrievalException.class)
    public void testCreateAndDeleteUser() throws UserCreationException, UserRetrievalException {
        String password = "password";
        String encryptedPassword = bCryptPasswordEncoder.encode(password);

        UserDTO testUser = new UserDTO();
        testUser.setAccountEnabled(true);
        testUser.setAccountExpired(false);
        testUser.setAccountLocked(false);
        testUser.setCredentialsExpired(false);
        testUser.setEmail("kekey@ainq.com");
        testUser.setFullName("Katy");
        testUser.setFriendlyName("Ekey-Test");
        testUser.setPhoneNumber("443-745-0987");
        testUser.setSubjectName("testUser");
        testUser.setTitle("Developer");
        testUser = dao.create(testUser, encryptedPassword);

        assertNotNull(testUser.getId());
        assertEquals("testUser", testUser.getSubjectName());

        Long insertedUserId = testUser.getId();
        dao.delete(insertedUserId);

        dao.getById(insertedUserId);
    }

    @Test
    public void testAddAcbAdminPermission() throws UserRetrievalException, UserPermissionRetrievalException {
        UserDTO toEdit = dao.getByName("TESTUSER");
        assertNotNull(toEdit);

        dao.removePermission(toEdit.getSubjectName(), ROLE_ACB);
        dao.addPermission(toEdit.getSubjectName(), ROLE_ACB);

        Set<UserPermissionDTO> permissions = permDao.findPermissionsForUser(toEdit.getId());
        assertNotNull(permissions);
        boolean hasAcbStaffRole = false;
        for (UserPermissionDTO perm : permissions) {
            if (ROLE_ACB.equals(perm.toString())) {
                hasAcbStaffRole = true;
            }
        }
        assertTrue(hasAcbStaffRole);
    }

    @Test
    public void testAddInvalidPermission() throws UserRetrievalException, UserPermissionRetrievalException {
        UserDTO toEdit = dao.getByName("TESTUSER");
        assertNotNull(toEdit);

        boolean caught = false;
        try {
            dao.addPermission(toEdit.getSubjectName(), "BOGUS");
        } catch (UserPermissionRetrievalException ex) {
            caught = true;
        }

        assertTrue(caught);
    }

    @Test
    public void testFindUser() {
        UserDTO toFind = new UserDTO();
        toFind.setSubjectName("TESTUSER");
        toFind.setFullName("TEST");
        toFind.setFriendlyName("USER");
        toFind.setEmail("test@ainq.com");
        toFind.setPhoneNumber("(301) 560-6999");
        toFind.setTitle("employee");

        UserDTO found = dao.findUser(toFind);
        assertNotNull(found);
        assertNotNull(found.getId());
        assertEquals(1, found.getId().longValue());

    }

    /**
     * Given the DAO is called 
     * When the passed in user id has been deleted 
     * Then null is returned
     * 
     * @throws UserRetrievalException
     */
    @Test(expected = UserRetrievalException.class)
    public void testGetById_returnsNullForDeletedUser() throws UserRetrievalException {
        dao.getById(-3L);
    }

    /**
     * Given the DAO is called 
     * When the passed in user id is valid/active 
     * Then a result is returned
     * 
     * @throws UserRetrievalException
     */
    @Test
    public void testGetById_returnsResultForActiveUser() throws UserRetrievalException {
        UserDTO userDto = null;
        userDto = dao.getById(-2L);
        assertTrue(userDto != null);
    }
}
