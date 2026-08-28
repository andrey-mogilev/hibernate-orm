/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.hibernate.KeyType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-15874")
@DomainModel(
		annotatedClasses = {
				OverriddenFieldAccessTest.ParentEntity.class,
				OverriddenFieldAccessTest.ThreeLevelOverrideMiddleEntity.class,
				OverriddenFieldAccessTest.WithOverridesInMiddleOnly.class,
				OverriddenFieldAccessTest.ThreeLevelOverrideMappedSuperclass.class,
				OverriddenFieldAccessTest.WithMappedSuperclassOverridesInMiddleOnly.class,
				OverriddenFieldAccessTest.WithFieldOverride.class,
				OverriddenFieldAccessTest.WithPropertyOverride.class,
				OverriddenFieldAccessTest.AssociatedEntity.class,
				OverriddenFieldAccessTest.ParentEntityWithComplexProperty.class,
				OverriddenFieldAccessTest.WithComplexPropertyOverride.class,
				OverriddenFieldAccessTest.WithIdOverride.class,
				OverriddenFieldAccessTest.ParentMappedSuperclass.class,
				OverriddenFieldAccessTest.WithMappedSuperclassOverrides.class,
				OverriddenFieldAccessTest.ParentEntityWithNonVirtualIdGetter.class,
				OverriddenFieldAccessTest.WithIdOverrideAndNonVirtualParentGetter.class,
				OverriddenFieldAccessTest.ParentEntityWithCompositeNaturalId.class,
				OverriddenFieldAccessTest.WithCompositeNaturalIdOverride.class,
				OverriddenFieldAccessTest.ParentEntityWithSimpleNaturalId.class,
				OverriddenFieldAccessTest.WithSimpleNaturalIdOverride.class,
				OverriddenFieldAccessTest.ParentEntityWithObjectId.class,
				OverriddenFieldAccessTest.WithDifferentTypeOverride.class,
				OverriddenFieldAccessTest.ParentEntityWithMixedAccess.class,
				OverriddenFieldAccessTest.WithReversedAccessOverrides.class,
				OverriddenFieldAccessTest.ParentEntityWithPrimitives.class,
				OverriddenFieldAccessTest.WithPrimitiveOverride.class,
				OverriddenFieldAccessTest.ParentEntityWithVersion.class,
				OverriddenFieldAccessTest.WithVersionOverride.class
		}
)
@SessionFactory
public class OverriddenFieldAccessTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void parentIsAccessedForOverriddenFieldWithoutGetter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithFieldOverride(
					"id",
					"parent value",
					"child value"
			);
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( WithFieldOverride.class, "id" );

			assertThat( entity ).isNotNull();
			assertThat( ( (ParentEntity) entity ).field ).isEqualTo( "parent value" );
			assertThat( entity.field ).isNull();
		} );
	}

	@Test
	void childIsAccessedForOverriddenFieldWithGetter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithPropertyOverride(
					"id",
					"parent property value",
					"child property value"
			);
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( WithPropertyOverride.class, "id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getProperty() ).isEqualTo( "child property value" );
		} );

		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithPropertyOverride.class )
				.getPropertyColumnNames( "property" ) )
				.containsExactly( "col_property" );
	}

	@Test
	void childIsAccessedForOverriddenComplexPropertyWithGetter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parentComplexProperty = new AssociatedEntity( "parent complex property" );
			final var childComplexProperty = new AssociatedEntity( "child complex property" );
			session.persist( parentComplexProperty );
			session.persist( childComplexProperty );
			session.persist(
					new WithComplexPropertyOverride(
							"id",
							parentComplexProperty,
							childComplexProperty
					)
			);
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( WithComplexPropertyOverride.class, "id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getComplexProperty().getId() ).isEqualTo( "child complex property" );
			assertThat( ( (ParentEntityWithComplexProperty) entity ).complexProperty ).isNull();
		} );
	}

	@Test
	void childIsAccessedForOverriddenIdFieldWithGetter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithIdOverride( "parent id", "child id" );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( WithIdOverride.class, "child id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "child id" );
		} );
	}

	@Test
	void middleIsAccessedForIdAndPropertyNotOverriddenInConcreteChild(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new WithOverridesInMiddleOnly(
						"parent id",
						"middle id",
						"parent property",
						"middle property"
				)
		) );

		scope.inTransaction( session -> {
			final var entity = session.find( WithOverridesInMiddleOnly.class, "middle id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "middle id" );
			assertThat( entity.getProperty() ).isEqualTo( "middle property" );
		} );

		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithOverridesInMiddleOnly.class )
				.getPropertyColumnNames( "property" ) )
				.containsExactly( "col_property" );
	}

	@Test
	void mappedSuperclassIsAccessedForIdAndPropertyNotOverriddenInConcreteChild(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new WithMappedSuperclassOverridesInMiddleOnly(
						"parent id",
						"mapped superclass id",
						"parent property",
						"mapped superclass property"
				)
		) );

		scope.inTransaction( session -> {
			final var entity = session.find(
					WithMappedSuperclassOverridesInMiddleOnly.class,
					"mapped superclass id"
			);

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "mapped superclass id" );
			assertThat( entity.getProperty() ).isEqualTo( "mapped superclass property" );
		} );

		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithMappedSuperclassOverridesInMiddleOnly.class )
				.getPropertyColumnNames( "property" ) )
				.containsExactly( "col_property" );
	}

	@Test
	void childIsAccessedForOverridesFromMappedSuperclass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithMappedSuperclassOverrides(
					"parent id",
					"child id",
					"parent field value",
					"child field value",
					"parent property value",
					"child property value"
			);
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( WithMappedSuperclassOverrides.class, "child id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getId() ).isEqualTo( "child id" );
			assertThat( entity.field ).isEqualTo( "child field value" );
			assertThat( entity.getProperty() ).isEqualTo( "child property value" );
			assertThat( ( (ParentMappedSuperclass) entity ).id ).isNull();
			assertThat( ( (ParentMappedSuperclass) entity ).field ).isNull();
			assertThat( ( (ParentMappedSuperclass) entity ).property ).isNull();
		} );
	}

	@Test
	void parentIsAccessedForOverriddenIdFieldWithNonVirtualParentGetter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithIdOverrideAndNonVirtualParentGetter( "parent id", "child id" );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find(
					WithIdOverrideAndNonVirtualParentGetter.class,
					"parent id"
			);

			assertThat( entity ).isNotNull();
			assertThat( ( (ParentEntityWithNonVirtualIdGetter) entity ).getId() ).isEqualTo( "parent id" );
			assertThat( entity.getId() ).isNull();
		} );
	}

	@Test
	void parentIsAccessedForOverriddenFieldsWithDifferentTypes(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithDifferentTypeOverride(
					"parent id",
					1,
					"parent property",
					2
			);
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( WithDifferentTypeOverride.class, "parent id" );

			assertThat( entity ).isNotNull();
			assertThat( ( (ParentEntityWithObjectId) entity ).id ).isEqualTo( "parent id" );
			assertThat( ( (ParentEntityWithObjectId) entity ).property ).isEqualTo( "parent property" );
			assertThat( entity.id ).isNull();
			assertThat( entity.property ).isNull();
		} );
	}

	@Test
	void childIsAccessedForOverridesWithDifferentAccessStrategies(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new WithReversedAccessOverrides(
						"id",
						"parent field-access property",
						"child property-access property",
						"parent property-access property",
						"child field-access property"
				)
		) );

		scope.inTransaction( session -> {
			final var entity = session.find( WithReversedAccessOverrides.class, "id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getFieldAccessProperty() ).isEqualTo( "child property-access property" );
			assertThat( entity.getPropertyAccessProperty() ).isEqualTo( "child field-access property" );
			assertThat( ( (ParentEntityWithMixedAccess) entity ).fieldAccessProperty ).isNull();
			assertThat( ( (ParentEntityWithMixedAccess) entity ).propertyAccessProperty ).isNull();
		} );

		final var entityDescriptor = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithReversedAccessOverrides.class );
		assertThat( entityDescriptor.getPropertyColumnNames( "fieldAccessProperty" ) )
				.containsExactly( "parent_field_access_property" );
		assertThat( entityDescriptor.getPropertyColumnNames( "propertyAccessProperty" ) )
				.containsExactly( "parent_property_access_property" );
	}

	@Test
	void childIsAccessedForOverriddenCompositeNaturalIdFieldWithGetter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithCompositeNaturalIdOverride(
					"part 1",
					"parent part 2",
					"child part 2"
			);

			final var naturalIdMapping = scope.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( WithCompositeNaturalIdOverride.class )
					.getNaturalIdMapping();
			assertThat( naturalIdMapping ).isNotNull();
			assertThat( (Object[]) naturalIdMapping.extractNaturalIdFromEntity( entity ) )
					.containsExactly( "part 1", "child part 2" );

			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find(
					WithCompositeNaturalIdOverride.class,
					Map.of( "naturalIdPart1", "part 1", "naturalIdPart2", "child part 2" ),
					KeyType.NATURAL
			);

			assertThat( entity ).isNotNull();
			assertThat( entity.getNaturalIdPart2() ).isEqualTo( "child part 2" );
		} );
	}

	@Test
	void childIsAccessedForOverriddenSimpleNaturalIdFieldWithGetter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new WithSimpleNaturalIdOverride( "parent natural id", "child natural id" );

			final var naturalIdMapping = scope.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( WithSimpleNaturalIdOverride.class )
					.getNaturalIdMapping();
			assertThat( naturalIdMapping ).isNotNull();
			assertThat( naturalIdMapping.extractNaturalIdFromEntity( entity ) )
					.isEqualTo( "child natural id" );

			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find(
					WithSimpleNaturalIdOverride.class,
					"child natural id",
					KeyType.NATURAL
			);

			assertThat( entity ).isNotNull();
			assertThat( entity.getNaturalId() ).isEqualTo( "child natural id" );
		} );
	}

	@Test
	void childIsAccessedForOverriddenPrimitiveFieldsWithGetters(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new WithPrimitiveOverride( "id", 1, 2L, 3, 4L )
		) );

		scope.inTransaction( session -> {
			final var entity = session.find( WithPrimitiveOverride.class, "id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getIntProperty() ).isEqualTo( 3 );
			assertThat( entity.getLongProperty() ).isEqualTo( 4L );
			assertThat( ( (ParentEntityWithPrimitives) entity ).intProperty ).isZero();
			assertThat( ( (ParentEntityWithPrimitives) entity ).longProperty ).isZero();
		} );
	}

	@Test
	void childIsAccessedForOverriddenVersionFieldWithGetter(SessionFactoryScope scope) {
		final var entityDescriptor = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( WithVersionOverride.class );

		// the version mapping must read the child field, consistently with the mapped attribute
		final var detached = new WithVersionOverride( "detached", "data" );
		( (ParentEntityWithVersion) detached ).version = 7;
		detached.version = 42;
		assertThat( entityDescriptor.getVersion( detached ) ).isEqualTo( 42 );

		scope.inTransaction( session -> session.persist( new WithVersionOverride( "id", "initial data" ) ) );

		scope.inTransaction( session -> {
			final var entity = session.find( WithVersionOverride.class, "id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.version ).isNotNull();
			assertThat( ( (ParentEntityWithVersion) entity ).version ).isNull();

			entity.setData( "updated data" );
		} );

		// the optimistic lock must be applied against the child field, otherwise the update either
		// fails with a StaleObjectStateException or silently loses the version increment
		scope.inTransaction( session -> {
			final var entity = session.find( WithVersionOverride.class, "id" );

			assertThat( entity ).isNotNull();
			assertThat( entity.getData() ).isEqualTo( "updated data" );
			assertThat( entity.version ).isEqualTo( 1 );
			assertThat( ( (ParentEntityWithVersion) entity ).version ).isNull();
		} );
	}

	@Entity(name = "ParentEntity")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntity {
		@Id
		private String id;

		private String field;

		@Column(name = "col_property")
		private String property;

		protected ParentEntity() {
		}

		protected ParentEntity(String id, String field, String property) {
			this.id = id;
			this.field = field;
			this.property = property;
		}

		public String getId() {
			return id;
		}

		public String getProperty() {
			return property;
		}
	}

	@Entity(name = "ThreeLevelOverrideMiddleEntity")
	public abstract static class ThreeLevelOverrideMiddleEntity extends ParentEntity {
		private String id;

		@Column(name = "annotation_should_be_ignored")
		private String property;

		protected ThreeLevelOverrideMiddleEntity() {
		}

		protected ThreeLevelOverrideMiddleEntity(
				String parentId,
				String middleId,
				String parentProperty,
				String middleProperty) {
			super( parentId, "field value", parentProperty );
			this.id = middleId;
			this.property = middleProperty;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getProperty() {
			return property;
		}
	}

	@Entity(name = "WithOverridesInMiddleOnly")
	public static class WithOverridesInMiddleOnly extends ThreeLevelOverrideMiddleEntity {
		protected WithOverridesInMiddleOnly() {
		}

		public WithOverridesInMiddleOnly(
				String parentId,
				String middleId,
				String parentProperty,
				String middleProperty) {
			super( parentId, middleId, parentProperty, middleProperty );
		}
	}

	@MappedSuperclass
	public abstract static class ThreeLevelOverrideMappedSuperclass extends ParentEntity {
		private String id;

		@Column(name = "annotation_should_be_ignored")
		private String property;

		protected ThreeLevelOverrideMappedSuperclass() {
		}

		protected ThreeLevelOverrideMappedSuperclass(
				String parentId,
				String mappedSuperclassId,
				String parentProperty,
				String mappedSuperclassProperty) {
			super( parentId, "field value", parentProperty );
			this.id = mappedSuperclassId;
			this.property = mappedSuperclassProperty;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getProperty() {
			return property;
		}
	}

	@Entity(name = "MappedSuperclassMiddleOverride")
	public static class WithMappedSuperclassOverridesInMiddleOnly extends ThreeLevelOverrideMappedSuperclass {
		protected WithMappedSuperclassOverridesInMiddleOnly() {
		}

		public WithMappedSuperclassOverridesInMiddleOnly(
				String parentId,
				String mappedSuperclassId,
				String parentProperty,
				String mappedSuperclassProperty) {
			super( parentId, mappedSuperclassId, parentProperty, mappedSuperclassProperty );
		}
	}

	@Entity(name = "WithFieldOverride")
	public static class WithFieldOverride extends ParentEntity {
		private String field;

		protected WithFieldOverride() {
		}

		public WithFieldOverride(String id, String parentField, String childField) {
			super( id, parentField, "property value" );
			this.field = childField;
		}
	}

	@Entity(name = "WithPropertyOverride")
	public static class WithPropertyOverride extends ParentEntity {
		@Column(name = "annotation_should_be_ignored")
		private String property;

		protected WithPropertyOverride() {
		}

		public WithPropertyOverride(String id, String parentProperty, String childProperty) {
			super( id, "field value", parentProperty );
			this.property = childProperty;
		}

		@Override
		public final String getProperty() {
			return property;
		}
	}

	@Entity(name = "AssociatedEntity")
	public static class AssociatedEntity {
		@Id
		private String id;

		protected AssociatedEntity() {
		}

		public AssociatedEntity(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	@Entity(name = "ParentEntityWithComplexProperty")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntityWithComplexProperty {
		@Id
		private String id;

		@ManyToOne
		@JoinColumn(name = "complex_property_id")
		private AssociatedEntity complexProperty;

		protected ParentEntityWithComplexProperty() {
		}

		protected ParentEntityWithComplexProperty(String id, AssociatedEntity complexProperty) {
			this.id = id;
			this.complexProperty = complexProperty;
		}

		public String getId() {
			return id;
		}

		public AssociatedEntity getComplexProperty() {
			return complexProperty;
		}
	}

	@Entity(name = "WithComplexPropertyOverride")
	public static class WithComplexPropertyOverride extends ParentEntityWithComplexProperty {
		private AssociatedEntity complexProperty;

		protected WithComplexPropertyOverride() {
		}

		public WithComplexPropertyOverride(
				String id,
				AssociatedEntity parentComplexProperty,
				AssociatedEntity childComplexProperty) {
			super( id, parentComplexProperty );
			this.complexProperty = childComplexProperty;
		}

		@Override
		public AssociatedEntity getComplexProperty() {
			return complexProperty;
		}
	}

	@Entity(name = "WithIdOverride")
	public static class WithIdOverride extends ParentEntity {
		private String id;

		protected WithIdOverride() {
		}

		public WithIdOverride(String parentId, String childId) {
			super( parentId, "field value", "property value" );
			this.id = childId;
		}

		@Override
		public String getId() {
			return id;
		}
	}

	@MappedSuperclass
	public abstract static class ParentMappedSuperclass {
		@Id
		private String id;
		private String field;
		private String property;

		protected ParentMappedSuperclass() {
		}

		protected ParentMappedSuperclass(String id, String field, String property) {
			this.id = id;
			this.field = field;
			this.property = property;
		}

		public String getId() {
			return id;
		}

		public String getProperty() {
			return property;
		}
	}

	@Entity(name = "WithMappedSuperclassOverrides")
	public static class WithMappedSuperclassOverrides extends ParentMappedSuperclass {
		private String id;
		private String field;
		private String property;

		protected WithMappedSuperclassOverrides() {
		}

		public WithMappedSuperclassOverrides(
				String parentId,
				String childId,
				String parentField,
				String childField,
				String parentProperty,
				String childProperty) {
			super( parentId, parentField, parentProperty );
			this.id = childId;
			this.field = childField;
			this.property = childProperty;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getProperty() {
			return property;
		}
	}

	@Entity(name = "ParentEntityWithNonVirtualIdGetter")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntityWithNonVirtualIdGetter {
		@Id
		private String id;

		protected ParentEntityWithNonVirtualIdGetter() {
		}

		protected ParentEntityWithNonVirtualIdGetter(String id) {
			this.id = id;
		}

		private String getId() {
			return id;
		}
	}

	@Entity(name = "WithNonVirtualIdGetter")
	public static class WithIdOverrideAndNonVirtualParentGetter extends ParentEntityWithNonVirtualIdGetter {
		private String id;

		protected WithIdOverrideAndNonVirtualParentGetter() {
		}

		public WithIdOverrideAndNonVirtualParentGetter(String parentId, String childId) {
			super( parentId );
			this.id = childId;
		}

		public String getId() {
			return id;
		}
	}

	@Entity(name = "ParentEntityWithSerializedId")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntityWithObjectId {
		@Id
		@JdbcTypeCode(SqlTypes.VARCHAR)
		private Object id;

		@JdbcTypeCode(SqlTypes.VARCHAR)
		private Object property;

		protected ParentEntityWithObjectId() {
		}

		protected ParentEntityWithObjectId(Object id, Object property) {
			this.id = id;
			this.property = property;
		}

		public Object getId() {
			return id;
		}

		public Object getProperty() {
			return property;
		}
	}

	@Entity(name = "WithDifferentIdTypeOverride")
	public static class WithDifferentTypeOverride extends ParentEntityWithObjectId {
		private Integer id;
		private Integer property;

		protected WithDifferentTypeOverride() {
		}

		public WithDifferentTypeOverride(
				Object parentId,
				Integer childId,
				Object parentProperty,
				Integer childProperty) {
			super( parentId, parentProperty );
			this.id = childId;
			this.property = childProperty;
		}

		@Override
		public Integer getId() {
			return id;
		}

		@Override
		public Integer getProperty() {
			return property;
		}
	}

	@Entity(name = "ParentEntityWithMixedAccess")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntityWithMixedAccess {
		@Id
		private String id;

		@Access(AccessType.FIELD)
		@Column(name = "parent_field_access_property")
		private String fieldAccessProperty;

		private String propertyAccessProperty;

		protected ParentEntityWithMixedAccess() {
		}

		protected ParentEntityWithMixedAccess(
				String id,
				String fieldAccessProperty,
				String propertyAccessProperty) {
			this.id = id;
			this.fieldAccessProperty = fieldAccessProperty;
			this.propertyAccessProperty = propertyAccessProperty;
		}

		public String getFieldAccessProperty() {
			return fieldAccessProperty;
		}

		@Access(AccessType.PROPERTY)
		@Column(name = "parent_property_access_property")
		public String getPropertyAccessProperty() {
			return propertyAccessProperty;
		}

		public void setPropertyAccessProperty(String propertyAccessProperty) {
			this.propertyAccessProperty = propertyAccessProperty;
		}
	}

	@Entity(name = "WithReversedAccessOverrides")
	public static class WithReversedAccessOverrides extends ParentEntityWithMixedAccess {
		private String fieldAccessProperty;

		@Access(AccessType.FIELD)
		@Column(name = "child_field_access_annotation_should_be_ignored")
		private String propertyAccessProperty;

		protected WithReversedAccessOverrides() {
		}

		public WithReversedAccessOverrides(
				String id,
				String parentFieldAccessProperty,
				String childPropertyAccessProperty,
				String parentPropertyAccessProperty,
				String childFieldAccessProperty) {
			super( id, parentFieldAccessProperty, parentPropertyAccessProperty );
			this.fieldAccessProperty = childPropertyAccessProperty;
			this.propertyAccessProperty = childFieldAccessProperty;
		}

		@Override
		@Access(AccessType.PROPERTY)
		@Column(name = "child_property_access_annotation_should_be_ignored")
		public String getFieldAccessProperty() {
			return fieldAccessProperty;
		}

		public void setFieldAccessProperty(String fieldAccessProperty) {
			this.fieldAccessProperty = fieldAccessProperty;
		}

		@Override
		public String getPropertyAccessProperty() {
			return propertyAccessProperty;
		}

		@Override
		public void setPropertyAccessProperty(String propertyAccessProperty) {
			this.propertyAccessProperty = propertyAccessProperty;
		}
	}

	@Entity(name = "ParentEntityWithCompositeNaturalId")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntityWithCompositeNaturalId {
		@Id
		private String id;

		@NaturalId
		private String naturalIdPart1;

		@NaturalId
		private String naturalIdPart2;

		protected ParentEntityWithCompositeNaturalId() {
		}

		protected ParentEntityWithCompositeNaturalId(
				String id,
				String naturalIdPart1,
				String naturalIdPart2) {
			this.id = id;
			this.naturalIdPart1 = naturalIdPart1;
			this.naturalIdPart2 = naturalIdPart2;
		}

		public String getId() {
			return id;
		}

		public String getNaturalIdPart1() {
			return naturalIdPart1;
		}

		public String getNaturalIdPart2() {
			return naturalIdPart2;
		}
	}

	@Entity(name = "WithCompositeNaturalIdOverride")
	public static class WithCompositeNaturalIdOverride extends ParentEntityWithCompositeNaturalId {
		private String naturalIdPart2;

		protected WithCompositeNaturalIdOverride() {
		}

		public WithCompositeNaturalIdOverride(
				String naturalIdPart1,
				String parentNaturalIdPart2,
				String childNaturalIdPart2) {
			super( "id", naturalIdPart1, parentNaturalIdPart2 );
			this.naturalIdPart2 = childNaturalIdPart2;
		}

		@Override
		public String getNaturalIdPart2() {
			return naturalIdPart2;
		}
	}

	@Entity(name = "ParentEntityWithSimpleNaturalId")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntityWithSimpleNaturalId {
		@Id
		private String id;

		@NaturalId
		private String naturalId;

		protected ParentEntityWithSimpleNaturalId() {
		}

		protected ParentEntityWithSimpleNaturalId(String id, String naturalId) {
			this.id = id;
			this.naturalId = naturalId;
		}

		public String getId() {
			return id;
		}

		public String getNaturalId() {
			return naturalId;
		}
	}

	@Entity(name = "WithSimpleNaturalIdOverride")
	public static class WithSimpleNaturalIdOverride extends ParentEntityWithSimpleNaturalId {
		private String naturalId;

		protected WithSimpleNaturalIdOverride() {
		}

		public WithSimpleNaturalIdOverride(String parentNaturalId, String childNaturalId) {
			super( "id", parentNaturalId );
			this.naturalId = childNaturalId;
		}

		@Override
		public String getNaturalId() {
			return naturalId;
		}
	}

	@Entity(name = "ParentEntityWithPrimitives")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntityWithPrimitives {
		@Id
		private String id;

		private int intProperty;

		private long longProperty;

		protected ParentEntityWithPrimitives() {
		}

		protected ParentEntityWithPrimitives(String id, int intProperty, long longProperty) {
			this.id = id;
			this.intProperty = intProperty;
			this.longProperty = longProperty;
		}

		public String getId() {
			return id;
		}

		public int getIntProperty() {
			return intProperty;
		}

		public long getLongProperty() {
			return longProperty;
		}
	}

	@Entity(name = "WithPrimitiveOverride")
	public static class WithPrimitiveOverride extends ParentEntityWithPrimitives {
		@Column(name = "annotation_should_be_ignored_int")
		private int intProperty;

		@Column(name = "annotation_should_be_ignored_long")
		private long longProperty;

		protected WithPrimitiveOverride() {
		}

		public WithPrimitiveOverride(
				String id,
				int parentIntProperty,
				long parentLongProperty,
				int childIntProperty,
				long childLongProperty) {
			super( id, parentIntProperty, parentLongProperty );
			this.intProperty = childIntProperty;
			this.longProperty = childLongProperty;
		}

		@Override
		public int getIntProperty() {
			return intProperty;
		}

		@Override
		public long getLongProperty() {
			return longProperty;
		}
	}

	@Entity(name = "ParentEntityWithVersion")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class ParentEntityWithVersion {
		@Id
		private String id;

		@Version
		private Integer version;

		private String data;

		protected ParentEntityWithVersion() {
		}

		protected ParentEntityWithVersion(String id, String data) {
			this.id = id;
			this.data = data;
		}

		public String getId() {
			return id;
		}

		public Integer getVersion() {
			return version;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity(name = "WithVersionOverride")
	public static class WithVersionOverride extends ParentEntityWithVersion {
		@Column(name = "annotation_should_be_ignored_version")
		private Integer version;

		protected WithVersionOverride() {
		}

		public WithVersionOverride(String id, String data) {
			super( id, data );
		}

		@Override
		public Integer getVersion() {
			return version;
		}
	}
}
