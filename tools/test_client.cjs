const path = require('path');
const mc = require(path.resolve(process.env.DTR_PROTOCOL_ROOT || path.resolve(__dirname, '../.tools/protocol'), 'node_modules/minecraft-protocol'));
const [,, port, version] = process.argv;
const data = require(path.resolve(process.env.DTR_PROTOCOL_ROOT || path.resolve(__dirname, '../.tools/protocol'), 'node_modules/minecraft-data'));
const protocol = data.versions.pc.find(v => v.minecraftVersion === version).version;
const schema = data(version)?.version.version === protocol ? version : data.supportedVersions.pc.find(v => data(v)?.version.version === protocol) || version;
const options = { host: '127.0.0.1', port: Number(port), username: 'Hash014Test', auth: 'offline', version: schema, disableChatSigning: true, clientSettings: { viewDistance: 2 } };
const client = mc.createClient(options);
// A few library aliases share a packet schema but incorrectly inherit its handshake number.
options.protocolVersion = protocol;
let maps = 0, lastPosition, mounted = false, moves = 0;
client.on('error', err => { console.error('CLIENT ERROR', err.stack); process.exitCode = 1; });
client.on('end', reason => { console.log('CLIENT END', reason, 'maps=' + maps); process.exit(); });
client.on('disconnect', p => console.log('CLIENT DISCONNECT', JSON.stringify(p)));
client.on('login', () => console.log('CLIENT LOGIN', client.version));
client.on('playerJoin', () => console.log('CLIENT JOIN'));
client.on('position', p => {
  if (p.teleportId !== undefined) client.write('teleport_confirm', { teleportId: p.teleportId });
  else if (version.startsWith('1.7.') || version.startsWith('1.8.')) {
    // Legacy clients acknowledge teleports with their feet/eye position.
    const feet = version.startsWith('1.7.') ? p.y - 1.62 : p.y;
    // minecraft-data 1.7 labels the wire feet/eye doubles in reverse order.
    // The installed PacketPlayInPositionLook reads feet first, then stance.
    lastPosition = { x: p.x, y: version.startsWith('1.7.') ? feet + 1.62 : feet,
      stance: version.startsWith('1.7.') ? feet : feet + 1.62,
      z: p.z, yaw: p.yaw, pitch: p.pitch, onGround: false };
    client.write('position_look', lastPosition);
  }
  if (Number(version.split('.')[1]) >= 9 && Number(version.split('.')[1]) <= 13) {
    lastPosition = { x: p.x, y: p.y, z: p.z, yaw: p.yaw, pitch: p.pitch, onGround: false };
    client.write('position_look', lastPosition);
  }
});
setInterval(() => { if (lastPosition && !mounted && client.state === 'play') client.write('position_look', lastPosition); }, 50).unref();
client.on('map', p => { maps++; console.log('CLIENT MAP', p.itemDamage ?? p.mapId, 'bytes=' + (p.data?.length ?? 0)); });
client.on('systemChat', p => { const text = JSON.stringify(p); if (text.includes('Reforged') || text.includes('Erreur')) console.log('CLIENT CHAT', text); });
setTimeout(() => { console.error('CLIENT TIMEOUT'); client.end(); }, 150000).unref();

client.on('spawn_entity_living', p => console.log('CLIENT SPAWN', p.entityId, 'type=' + p.type));
client.on('spawn_entity', p => console.log('CLIENT SPAWN', p.entityId, 'type=' + p.type));
client.on('set_passengers', p => { mounted = (p.passengers || []).length > 0; console.log('CLIENT MOUNT', p.entityId, JSON.stringify(p.passengers)); });
client.on('attach_entity', p => { mounted = p.vehicleId !== -1; console.log('CLIENT MOUNT', p.entityId, 'vehicle=' + p.vehicleId); });
for (const event of ['rel_entity_move','entity_move_look','entity_teleport','entity_position_sync']) client.on(event, p => { moves++; if(moves <= 30) console.log('CLIENT MOVE', p.entityId); });
