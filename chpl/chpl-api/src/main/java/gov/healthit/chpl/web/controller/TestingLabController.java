package gov.healthit.chpl.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Permission;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.healthit.chpl.auth.dto.UserDTO;
import gov.healthit.chpl.auth.dto.UserPermissionDTO;
import gov.healthit.chpl.auth.json.User;
import gov.healthit.chpl.auth.manager.UserManager;
import gov.healthit.chpl.auth.user.UserRetrievalException;
import gov.healthit.chpl.caching.CacheNames;
import gov.healthit.chpl.domain.ChplPermission;
import gov.healthit.chpl.domain.PermittedUser;
import gov.healthit.chpl.domain.TestingLab;
import gov.healthit.chpl.domain.UpdateUserAndAtlRequest;
import gov.healthit.chpl.dto.AddressDTO;
import gov.healthit.chpl.dto.TestingLabDTO;
import gov.healthit.chpl.exception.EntityCreationException;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.exception.InvalidArgumentsException;
import gov.healthit.chpl.manager.TestingLabManager;
import gov.healthit.chpl.manager.impl.UpdateTestingLabException;
import gov.healthit.chpl.web.controller.results.PermittedUserResults;
import gov.healthit.chpl.web.controller.results.TestingLabResults;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "atls")
@RestController
@RequestMapping("/atls")
public class TestingLabController {

    @Autowired
    private TestingLabManager atlManager;

    @Autowired
    private UserManager userManager;

    @ApiOperation(value = "List all testing labs (ATLs).",
            notes = "Setting the 'editable' parameter to true will return all ATLs that the logged in user has edit "
                    + "permissions on.  Setting 'showDeleted' to true will include even those ATLs that have been "
                    + "deleted. The logged in user must have ROLE_ADMIN to see deleted ATLs. The default behavior of "
                    + "this service is to list all of the ATLs in the system that are not deleted.")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody TestingLabResults getAtls(
            @RequestParam(required = false, defaultValue = "false") final boolean editable) {
        TestingLabResults results = new TestingLabResults();
        List<TestingLabDTO> atls = null;
        if (editable) {
            atls = atlManager.getAllForUser();
        } else {
            atls = atlManager.getAll();
        }

        if (atls != null) {
            for (TestingLabDTO atl : atls) {
                results.getAtls().add(new TestingLab(atl));
            }
        }
        return results;
    }

    @ApiOperation(value = "Get details about a specific testing lab (ATL).",
            notes = "The logged in user must have ROLE_ADMIN or have either read or"
                    + "administrative authority on the testing lab with the ID specified.")
    @RequestMapping(value = "/{atlId}", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody TestingLab getAtlById(@PathVariable("atlId") final Long atlId)
            throws EntityRetrievalException {
        TestingLabDTO atl = atlManager.getById(atlId);

        return new TestingLab(atl);
    }

    @ApiOperation(value = "Create a new testing lab.",
            notes = "The logged in user must have ROLE_ADMIN to create a new testing lab.")
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = "application/json; charset=utf-8")
    public TestingLab createAtl(@RequestBody final TestingLab atlInfo)
            throws InvalidArgumentsException, UserRetrievalException, EntityRetrievalException,
            EntityCreationException, JsonProcessingException {

        return create(atlInfo);
    }

    private TestingLab create(final TestingLab atlInfo) throws InvalidArgumentsException, UserRetrievalException,
    EntityRetrievalException, EntityCreationException, JsonProcessingException {
        TestingLabDTO toCreate = new TestingLabDTO();
        toCreate.setTestingLabCode(atlInfo.getAtlCode());
        toCreate.setAccredidationNumber(atlInfo.getAccredidationNumber());
        if (StringUtils.isEmpty(atlInfo.getName())) {
            throw new InvalidArgumentsException("A name is required for a testing lab");
        }
        toCreate.setName(atlInfo.getName());
        toCreate.setWebsite(atlInfo.getWebsite());

        if (atlInfo.getAddress() == null) {
            throw new InvalidArgumentsException("An address is required for a new testing lab");
        }
        AddressDTO address = new AddressDTO();
        address.setId(atlInfo.getAddress().getAddressId());
        address.setStreetLineOne(atlInfo.getAddress().getLine1());
        address.setStreetLineTwo(atlInfo.getAddress().getLine2());
        address.setCity(atlInfo.getAddress().getCity());
        address.setState(atlInfo.getAddress().getState());
        address.setZipcode(atlInfo.getAddress().getZipcode());
        address.setCountry(atlInfo.getAddress().getCountry());
        toCreate.setAddress(address);
        toCreate = atlManager.create(toCreate);
        return new TestingLab(toCreate);
    }

    @ApiOperation(value = "Update an existing ATL.",
            notes = "The logged in user must either have ROLE_ADMIN or have administrative "
                    + "authority on the testing lab whose data is being updated.")
    @RequestMapping(value = "/{atlId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = "application/json; charset=utf-8")
    public ResponseEntity<TestingLab> updateAtl(@RequestBody final TestingLab atlInfo)
            throws InvalidArgumentsException, EntityRetrievalException, JsonProcessingException,
            EntityCreationException, UpdateTestingLabException {

        return update(atlInfo);
    }

    private ResponseEntity<TestingLab> update(final TestingLab updatedAtl) throws InvalidArgumentsException,
    EntityRetrievalException, JsonProcessingException, EntityCreationException, UpdateTestingLabException {
        //get the ATL as it is currently in the database to find out if
        //the retired flag was changed.
        //Retirement and un-retirement is done as a separate manager action because
        //security is different from normal ATL updates - only admins are allowed
        //whereas an ATL admin can update other info
        TestingLabDTO existingAtl = atlManager.getIfPermissionById(updatedAtl.getId());
        if (existingAtl.isRetired() != updatedAtl.isRetired() && updatedAtl.isRetired()) {
            //we are retiring this ATL and no other changes can be made
            atlManager.retire(updatedAtl.getId());
        } else {
            if (existingAtl.isRetired() != updatedAtl.isRetired() && !updatedAtl.isRetired()) {
                //unretire the ATL
                atlManager.unretire(updatedAtl.getId());
            }
            TestingLabDTO toUpdate = new TestingLabDTO();
            toUpdate.setId(updatedAtl.getId());
            toUpdate.setTestingLabCode(updatedAtl.getAtlCode());
            toUpdate.setRetired(updatedAtl.isRetired());
            toUpdate.setAccredidationNumber(updatedAtl.getAccredidationNumber());
            if (StringUtils.isEmpty(updatedAtl.getName())) {
                throw new InvalidArgumentsException("A name is required for a testing lab");
            }
            toUpdate.setName(updatedAtl.getName());
            toUpdate.setWebsite(updatedAtl.getWebsite());

            if (updatedAtl.getAddress() == null) {
                throw new InvalidArgumentsException("An address is required to update the testing lab");
            }
            AddressDTO address = new AddressDTO();
            address.setId(updatedAtl.getAddress().getAddressId());
            address.setStreetLineOne(updatedAtl.getAddress().getLine1());
            address.setStreetLineTwo(updatedAtl.getAddress().getLine2());
            address.setCity(updatedAtl.getAddress().getCity());
            address.setState(updatedAtl.getAddress().getState());
            address.setZipcode(updatedAtl.getAddress().getZipcode());
            address.setCountry(updatedAtl.getAddress().getCountry());
            toUpdate.setAddress(address);
            atlManager.update(toUpdate);
        }

        TestingLabDTO result = atlManager.getIfPermissionById(updatedAtl.getId());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Cache-cleared", CacheNames.COLLECTIONS_LISTINGS);
        TestingLab response = new TestingLab(result);
        return new ResponseEntity<TestingLab>(response, responseHeaders, HttpStatus.OK);
    }

    @ApiOperation(value = "Remove user permissions from an ATL.",
            notes = "The logged in user must have ROLE_ADMIN or ROLE_ATL and have administrative authority on the "
                    + " specified ATL. The user specified in the request will have all authorities "
                    + " removed that are associated with the specified ATL.")
    @RequestMapping(value = "{atlId}/users/{userId}", method = RequestMethod.DELETE, 
    produces = "application/json; charset=utf-8")
    public String deleteUserFromAtl(@PathVariable final Long atlId, @PathVariable final Long userId)
            throws UserRetrievalException, EntityRetrievalException, InvalidArgumentsException {

        return deleteUser(atlId, userId);
    }

    private String deleteUser(final Long atlId, final Long userId)
            throws UserRetrievalException, EntityRetrievalException, InvalidArgumentsException {

        UserDTO user = userManager.getById(userId);
        TestingLabDTO atl = atlManager.getIfPermissionById(atlId);

        if (user == null || atl == null) {
            throw new InvalidArgumentsException("Could not find either ATL or User specified");
        }

        // delete all permissions on that atl
        atlManager.deleteAllPermissionsOnAtl(atl, new PrincipalSid(user.getSubjectName()));

        return "{\"userDeleted\" : true}";
    }

    @ApiOperation(value = "List users with permissions on a specified ATL.",
            notes = "The logged in user must have ROLE_ADMIN or have administrative or read authority on the "
                    + " specified ATL.")
    @RequestMapping(value = "/{atlId}/users", method = RequestMethod.GET,
    produces = "application/json; charset=utf-8")
    public @ResponseBody PermittedUserResults getUsers(@PathVariable("atlId") final Long atlId)
            throws InvalidArgumentsException, EntityRetrievalException {
        TestingLabDTO atl = atlManager.getIfPermissionById(atlId);
        if (atl == null) {
            throw new InvalidArgumentsException("Could not find the ATL specified.");
        }

        List<PermittedUser> atlUsers = new ArrayList<PermittedUser>();
        List<UserDTO> users = atlManager.getAllUsersOnAtl(atl);
        for (UserDTO user : users) {

            // only show users that have ROLE_ATL
            Set<UserPermissionDTO> systemPermissions = userManager.getGrantedPermissionsForUser(user);
            boolean hasAtlPermission = false;
            for (UserPermissionDTO systemPermission : systemPermissions) {
                if (systemPermission.getAuthority().equals("ROLE_ATL")) {
                    hasAtlPermission = true;
                }
            }

            if (hasAtlPermission) {
                List<String> roleNames = new ArrayList<String>();
                for (UserPermissionDTO role : systemPermissions) {
                    roleNames.add(role.getAuthority());
                }

                List<Permission> permissions = atlManager.getPermissionsForUser(atl,
                        new PrincipalSid(user.getSubjectName()));
                List<String> atlPerm = new ArrayList<String>(permissions.size());
                for (Permission permission : permissions) {
                    ChplPermission perm = ChplPermission.fromPermission(permission);
                    if (perm != null) {
                        atlPerm.add(perm.toString());
                    }
                }

                PermittedUser userInfo = new PermittedUser();
                userInfo.setUser(new User(user));
                userInfo.setPermissions(atlPerm);
                userInfo.setRoles(roleNames);
                atlUsers.add(userInfo);
            }
        }

        PermittedUserResults results = new PermittedUserResults();
        results.setUsers(atlUsers);
        return results;
    }
}
