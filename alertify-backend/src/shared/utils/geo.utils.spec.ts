import { calculateHaversineDistance, isWithinRadius } from '../../shared/utils/geo.utils';

describe('GeoUtils', () => {
  describe('calculateHaversineDistance', () => {
    it('debería calcular correctamente la distancia entre dos puntos', () => {
      // Cartagena, Colombia
      const lat1 = 10.3932;
      const lon1 = -75.4898;

      // Santa Marta, Colombia (aproximadamente 130 km)
      const lat2 = 11.2456;
      const lon2 = -74.2302;

      const distance = calculateHaversineDistance(lat1, lon1, lat2, lon2);

      // La distancia debería estar entre 120-140 km
      expect(distance).toBeGreaterThan(120000);
      expect(distance).toBeLessThan(140000);
    });

    it('debería retornar 0 para el mismo punto', () => {
      const distance = calculateHaversineDistance(10.3932, -75.4898, 10.3932, -75.4898);
      expect(distance).toBe(0);
    });
  });

  describe('isWithinRadius', () => {
    it('debería retornar true si el punto está dentro del radio', () => {
      const result = isWithinRadius(
        10.3932,
        -75.4898,
        10.39325, // Muy cerca
        -75.48985,
        500 // 500 metros
      );

      expect(result).toBe(true);
    });

    it('debería retornar false si el punto está fuera del radio', () => {
      const result = isWithinRadius(
        10.3932,
        -75.4898,
        11.2456, // ~130 km
        -74.2302,
        50000 // 50 km
      );

      expect(result).toBe(false);
    });
  });
});
