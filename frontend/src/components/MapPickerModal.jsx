import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

function MapPickerModal({ open, onClose, onConfirm, }) {
    const mapElement = useRef(null);
    const map = useRef(null);
    const marker = useRef(null);
    const [query, setQuery] = useState('');
    const [address, setAddress] = useState('');
    const [coordinates, setCoordinates] = useState(null);
    const [searching, setSearching] = useState(false);
    const [error, setError] = useState('');
    async function reverseGeocode(lat, lng) {
        try {
            const params = new URLSearchParams({ format: 'jsonv2', lat: String(lat), lon: String(lng) });
            const response = await fetch(`https://nominatim.openstreetmap.org/reverse?${params}`);
            if (!response.ok)
                throw new Error('Không tìm được địa chỉ tại vị trí này.');
            const result = await response.json();
            setAddress(result.display_name || `${lat.toFixed(6)}, ${lng.toFixed(6)}`);
            setError('');
        }
        catch (reason) {
            setAddress(`${lat.toFixed(6)}, ${lng.toFixed(6)}`);
            setError(reason instanceof Error ? reason.message : 'Không thể tìm địa chỉ.');
        }
    }
    function selectPoint(lat, lng, label) {
        const next = { lat, lng };
        setCoordinates(next);
        setAddress(label || `${lat.toFixed(6)}, ${lng.toFixed(6)}`);
        if (map.current) {
            map.current.setView([lat, lng], 16);
            marker.current?.remove();
            marker.current = L.circleMarker([lat, lng], {
                radius: 8,
                color: '#d85a30',
                weight: 3,
                fillColor: '#faece7',
                fillOpacity: 1,
            }).addTo(map.current);
        }
        if (!label)
            void reverseGeocode(lat, lng);
    }
    useEffect(() => {
        if (!open || !mapElement.current || map.current)
            return;
        const instance = L.map(mapElement.current).setView([21.0285, 105.8542], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; OpenStreetMap contributors',
        }).addTo(instance);
        instance.on('click', (event) => {
            selectPoint(event.latlng.lat, event.latlng.lng);
        });
        map.current = instance;
        const resize = window.setTimeout(() => instance.invalidateSize(), 100);
        return () => window.clearTimeout(resize);
    }, [open]);
    useEffect(() => {
        if (open)
            return;
        map.current?.remove();
        map.current = null;
        marker.current = null;
    }, [open]);
    async function searchAddress(event) {
        event.preventDefault();
        if (!query.trim())
            return;
        setSearching(true);
        setError('');
        try {
            const params = new URLSearchParams({ format: 'jsonv2', q: query.trim(), limit: '1', countrycodes: 'vn' });
            const response = await fetch(`https://nominatim.openstreetmap.org/search?${params}`);
            if (!response.ok)
                throw new Error('Không thể tìm địa chỉ lúc này.');
            const results = await response.json();
            if (!results.length)
                throw new Error('Không tìm thấy địa chỉ phù hợp.');
            const result = results[0];
            selectPoint(Number(result.lat), Number(result.lon), result.display_name);
        }
        catch (reason) {
            setError(reason instanceof Error ? reason.message : 'Không thể tìm địa chỉ.');
        }
        finally {
            setSearching(false);
        }
    }
    function locateMe() {
        if (!navigator.geolocation) {
            setError('Trình duyệt này không hỗ trợ định vị.');
            return;
        }
        navigator.geolocation.getCurrentPosition((position) => selectPoint(position.coords.latitude, position.coords.longitude), () => setError('Không thể lấy vị trí hiện tại. Hãy cho phép truy cập vị trí hoặc tìm địa chỉ.'));
    }
    if (!open)
        return null;
    return (<div className="map-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget)
        onClose(); }}>
      <section className="map-modal" role="dialog" aria-modal="true" aria-labelledby="map-title">
        <div className="map-header"><h3 id="map-title"><i className="ti ti-map-pin" aria-hidden="true"/> Chọn vị trí giao hàng</h3><button type="button" className="close-map" onClick={onClose} aria-label="Đóng">×</button></div>
        <form className="map-search" onSubmit={searchAddress}><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm địa chỉ, tên đường, tòa nhà..." aria-label="Tìm địa chỉ"/><button type="submit" disabled={searching} aria-label="Tìm kiếm"><i className="ti ti-search" aria-hidden="true"/></button><button type="button" className="locate-button" onClick={locateMe} aria-label="Vị trí hiện tại"><i className="ti ti-current-location" aria-hidden="true"/></button></form>
        {error && <p className="map-error" role="alert">{error}</p>}
        <div ref={mapElement} className="leaflet-map"/>
        <div className="map-footer"><div className="map-address"><strong>Vị trí đã chọn:</strong><span>{address || 'Nhấp vào bản đồ hoặc tìm kiếm để chọn vị trí.'}</span>{coordinates && <small>{coordinates.lat.toFixed(6)}, {coordinates.lng.toFixed(6)}</small>}</div><button type="button" className="confirm-location" disabled={!coordinates || !address} onClick={() => { if (coordinates && address)
        onConfirm(address, coordinates); }}>Xác nhận</button></div>
      </section>
    </div>);
}

export default MapPickerModal;