import java.util.*;

import javax.persistence.*;

@Entity
public class OxModule {

	@Id
	@Column(unique = true, nullable = false)
	int id;

	@Column(unique = true, nullable = false, length = 50)
	String name;
	
	@Column(length = 50)
	String description;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "modules")
	Collection<OxUser> users;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Collection<OxUser> getUsers() {
		return users;
	}

	public void setUsers(Collection<OxUser> users) {
		this.users = users;
	}

	@Override
	public boolean equals(Object o) {
		return o != null && o instanceof OxModule
				&& this.getId() == ((OxModule)o).getId();
	}

	@Override
	public int hashCode() {
		return this.getId();
	}
}
