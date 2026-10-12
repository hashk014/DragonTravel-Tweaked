package fr.hash014.dragontravel;

import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.metadata.FixedMetadataValue;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

/** No versioned NMS imports: subclass the installed dragon, suppress its autonomous AI. */
public final class DragonAdapter {
  public static final String TAG = "dragontravel-reforged";
  private final DragonTravelReforged plugin;
  private Constructor<?> constructor;
  private Object entityType;
  private final Map<Class<?>, Method> movers = new HashMap<Class<?>, Method>();
  private final List<Map<Object, Object>> legacyMaps = new ArrayList<Map<Object, Object>>();
  private final Map<UUID, Chunk> anchors = new HashMap<UUID, Chunk>();
  private final Map<UUID, Entity> anchoredEntities = new HashMap<UUID, Entity>();
  private final Map<Chunk, Integer> leases = new HashMap<Chunk, Integer>();

  public DragonAdapter(DragonTravelReforged plugin) {
    this.plugin = plugin;
  }

  public static Object handle(Object object) throws ReflectiveOperationException {
    return object.getClass().getMethod("getHandle").invoke(object);
  }

  public Entity spawn(Location location, String label) throws Exception {
    location.getChunk().load();
    World world = location.getWorld();
    Object nativeWorld = handle(world);
    if (constructor == null) initialize(location, nativeWorld);
    Object dragon =
        constructor.getParameterTypes().length == 1
            ? constructor.newInstance(nativeWorld)
            : constructor.newInstance(entityType, nativeWorld);
    Entity entity = (Entity) dragon.getClass().getMethod("getBukkitEntity").invoke(dragon);
    entity.setMetadata(TAG, new FixedMetadataValue(plugin, true));
    ((LivingEntity) entity)
        .setCustomName(
            "DTR#"
                + entity.getUniqueId()
                + " | "
                + ChatColor.translateAlternateColorCodes('&', label));
    ((LivingEntity) entity).setCustomNameVisible(false);
    optional(entity, "setPersistent", new Class<?>[] {boolean.class}, false);
    optional(entity, "setInvulnerable", new Class<?>[] {boolean.class}, true);
    optional(entity, "setSilent", new Class<?>[] {boolean.class}, true);
    move(entity, location);
    boolean added = false;
    try {
      for (Method m : world.getClass().getMethods()) {
        Class<?>[] p = m.getParameterTypes();
        if (m.getName().equals("addEntity")
            && p.length == 2
            && p[0].isInstance(dragon)
            && p[1] == SpawnReason.class) {
          Object result = m.invoke(world, dragon, SpawnReason.CUSTOM);
          added = !(result instanceof Boolean) || ((Boolean) result);
          break;
        }
      }
      if (!added)
        for (Method m : nativeWorld.getClass().getMethods()) {
          Class<?>[] p = m.getParameterTypes();
          if ((m.getName().equals("addEntity") || m.getName().equals("addFreshEntity"))
              && p.length == 2
              && p[0].isInstance(dragon)
              && p[1] == SpawnReason.class) {
            Object result = m.invoke(nativeWorld, dragon, SpawnReason.CUSTOM);
            added = !(result instanceof Boolean) || ((Boolean) result);
            break;
          }
        }
      if (!added)
        for (Method m : nativeWorld.getClass().getMethods()) {
          Class<?>[] p = m.getParameterTypes();
          if ((m.getName().equals("addEntity") || m.getName().equals("addFreshEntity"))
              && p.length == 1
              && p[0].isInstance(dragon)) {
            Object result = m.invoke(nativeWorld, dragon);
            added = !(result instanceof Boolean) || ((Boolean) result);
            break;
          }
        }
      // 1.17-1.19 can mark entities untracked while their chunk visibility
      // transitions; isDead distinguishes removal from this transient state.
      if (!added || entity.isDead()) throw new IllegalStateException("Dragon spawn refused");
      return entity;
    } catch (Exception e) {
      entity.remove();
      throw e;
    }
  }

  private void initialize(Location location, Object nativeWorld) throws Exception {
    Object sample = null;
    // createEntity avoids inserting a vanilla dragon into the world on recent servers.
    for (Method m : location.getWorld().getClass().getMethods()) {
      if (m.getName().equals("createEntity")
          && Arrays.equals(m.getParameterTypes(), new Class<?>[] {Location.class, Class.class})) {
        sample = m.invoke(location.getWorld(), location, EnderDragon.class);
        if (sample instanceof Entity) sample = handle(sample);
        break;
      }
    }
    if (sample == null) {
      Entity probe = location.getWorld().spawnEntity(location, EntityType.ENDER_DRAGON);
      sample = handle(probe);
      probe.remove();
    }
    Class<?> base = sample.getClass();
    Constructor<?> superCtor = null;
    for (Constructor<?> c : base.getConstructors()) {
      Class<?>[] p = c.getParameterTypes();
      if ((p.length == 1 && p[0].isInstance(nativeWorld))
          || (p.length == 2 && p[1].isInstance(nativeWorld))) {
        superCtor = c;
        break;
      }
    }
    if (superCtor == null)
      throw new IllegalStateException("Unsupported dragon constructor: " + base.getName());
    if (superCtor.getParameterTypes().length == 2) {
      Class<?> type = superCtor.getParameterTypes()[0];
      for (Method m : base.getMethods())
        if (m.getParameterTypes().length == 0 && type.isAssignableFrom(m.getReturnType())) {
          entityType = m.invoke(sample);
          if (entityType != null) break;
        }
      if (entityType == null)
        for (Field f : type.getFields())
          if (Modifier.isStatic(f.getModifiers())
              && type.isAssignableFrom(f.getType())
              && (f.getName().equals("ENDER_DRAGON") || f.getName().equals("ENDERDRAGON")))
            entityType = f.get(null);
      if (entityType == null) throw new IllegalStateException("Dragon entity type not found");
    }
    String name = "fr/hash014/dragontravel/generated/ControlledDragon",
        parent = Type.getInternalName(base);
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null, parent, null);
    String desc = Type.getConstructorDescriptor(superCtor);
    MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
    mv.visitCode();
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    for (int i = 0; i < superCtor.getParameterTypes().length; i++)
      mv.visitVarInsn(Opcodes.ALOAD, i + 1);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parent, "<init>", desc, false);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    List<String> suppressed = new ArrayList<String>();
    // Preserve all virtual routines called during any superclass constructor
    // (legacy attributes and DataWatcher initialization are among them).
    final Set<String> construction = new HashSet<String>();
    for (Class<?> c = base; c != null && c != Object.class; c = c.getSuperclass()) {
      java.io.InputStream stream = c.getResourceAsStream("/" + Type.getInternalName(c) + ".class");
      if (stream == null)
        throw new IllegalStateException("Cannot inspect constructor: " + c.getName());
      try {
        new ClassReader(stream)
            .accept(
                new ClassVisitor(Opcodes.ASM9) {
                  @Override
                  public MethodVisitor visitMethod(
                      int access,
                      String name,
                      String descriptor,
                      String signature,
                      String[] exceptions) {
                    if (!name.equals("<init>")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                      @Override
                      public void visitMethodInsn(
                          int opcode, String owner, String name, String desc, boolean itf) {
                        if (desc.equals("()V")) construction.add(name);
                      }
                    };
                  }
                },
                ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      } finally {
        stream.close();
      }
    }
    for (Method method : base.getDeclaredMethods()) {
      int flags = method.getModifiers();
      if (method.getParameterTypes().length != 0
          || method.getReturnType() != void.class
          || construction.contains(method.getName())
          || Modifier.isStatic(flags)
          || Modifier.isFinal(flags)
          || Modifier.isPrivate(flags)
          || !(Modifier.isPublic(flags) || Modifier.isProtected(flags))) continue;
      mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), "()V", null, null);
      mv.visitCode();
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
      suppressed.add(method.getName());
    }
    if (suppressed.isEmpty()) throw new IllegalStateException("No dragon AI routines identified");
    cw.visitEnd();
    class Loader extends ClassLoader {
      Loader() {
        super(base.getClassLoader());
      }

      Class<?> define(byte[] b) {
        return defineClass(null, b, 0, b.length);
      }
    }
    Class<?> controlled = new Loader().define(cw.toByteArray());
    constructor = controlled.getConstructor(superCtor.getParameterTypes());
    // Legacy trackers resolve packet IDs by exact class. Add aliases, never replace vanilla
    // factories.
    if (superCtor.getParameterTypes().length == 1) {
      Class<?> registry = Class.forName(base.getPackage().getName() + ".EntityTypes");
      for (Field f : registry.getDeclaredFields())
        if (Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())) {
          f.setAccessible(true);
          @SuppressWarnings("unchecked")
          Map<Object, Object> map = (Map<Object, Object>) f.get(null);
          if (map.containsKey(base)) {
            map.put(controlled, map.get(base));
            legacyMaps.add(map);
          }
        }
    }
    plugin
        .getLogger()
        .info("Dragon adapter: " + base.getName() + " controlled routines=" + suppressed);
  }

  public void move(Entity entity, Location to) throws Exception {
    if (!entity.getWorld().equals(to.getWorld()))
      throw new IllegalArgumentException("Movement must remain in one world");
    to.getChunk().load();
    if (entity.getType() == EntityType.ENDER_DRAGON && entity.hasMetadata(TAG))
      pin(entity, to.getChunk());
    Object nativeEntity = handle(entity);
    Class<?> cls = nativeEntity.getClass();
    Method mover = movers.get(cls);
    if (mover == null) {
      for (String name :
          Arrays.asList(
              "snapTo", "moveTo", "setPositionRotation", "setLocation", "absMoveTo", "b", "a")) {
        try {
          mover =
              cls.getMethod(
                  name, double.class, double.class, double.class, float.class, float.class);
          break;
        } catch (NoSuchMethodException ignored) {
        }
      }
      if (mover == null)
        throw new IllegalStateException("No absolute position adapter for " + cls.getName());
      movers.put(cls, mover);
    }
    mover.invoke(nativeEntity, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
    entity.setFallDistance(0);
  }

  public boolean mount(Entity dragon, Entity passenger) throws Exception {
    try {
      return (Boolean)
          dragon.getClass().getMethod("addPassenger", Entity.class).invoke(dragon, passenger);
    } catch (NoSuchMethodException e) {
      return dragon.setPassenger(passenger);
    }
  }

  private void pin(Entity entity, Chunk chunk) throws Exception {
    UUID id = entity.getUniqueId();
    Chunk previous = anchors.get(id);
    if (chunk.equals(previous)) return;
    if (!leases.containsKey(chunk)) ticket(chunk, true);
    leases.put(chunk, leases.containsKey(chunk) ? leases.get(chunk) + 1 : 1);
    anchors.put(id, chunk);
    anchoredEntities.put(id, entity);
    if (previous != null) release(previous);
  }

  private void ticket(Chunk chunk, boolean add) throws Exception {
    try {
      chunk
          .getClass()
          .getMethod(
              add ? "addPluginChunkTicket" : "removePluginChunkTicket",
              org.bukkit.plugin.Plugin.class)
          .invoke(chunk, plugin);
    } catch (NoSuchMethodException ignored) {
      /* Legacy lease honored by ChunkUnloadEvent. */
    }
  }

  private void release(Chunk chunk) throws Exception {
    int count = leases.get(chunk) - 1;
    if (count == 0) {
      leases.remove(chunk);
      ticket(chunk, false);
    } else leases.put(chunk, count);
  }

  public boolean held(Chunk chunk) {
    return leases.containsKey(chunk);
  }

  public void prune() {
    for (UUID id : new ArrayList<UUID>(anchors.keySet()))
      if (anchoredEntities.get(id).isDead()) {
        Chunk chunk = anchors.remove(id);
        anchoredEntities.remove(id);
        try {
          release(chunk);
        } catch (Exception ex) {
          plugin.getLogger().warning(ex.toString());
        }
      }
  }

  public void close() {
    for (Chunk chunk : new ArrayList<Chunk>(leases.keySet()))
      try {
        ticket(chunk, false);
      } catch (Exception ex) {
        plugin.getLogger().warning(ex.toString());
      }
    leases.clear();
    anchors.clear();
    anchoredEntities.clear();
    if (constructor != null)
      for (Map<Object, Object> map : legacyMaps) map.remove(constructor.getDeclaringClass());
  }

  public static boolean owned(Entity entity) {
    if (entity.hasMetadata(TAG)) return true;
    if (entity.getType() != EntityType.ENDER_DRAGON) return false;
    String name = ((LivingEntity) entity).getCustomName();
    return name != null && name.startsWith("DTR#");
  }

  private static void optional(Object obj, String name, Class<?>[] types, Object... values)
      throws Exception {
    try {
      obj.getClass().getMethod(name, types).invoke(obj, values);
    } catch (NoSuchMethodException ignored) {
    }
  }
}
