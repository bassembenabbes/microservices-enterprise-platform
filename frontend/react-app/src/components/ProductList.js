import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchProducts, searchProducts } from '../store/slices/productSlice';

const ProductList = () => {
  const dispatch = useDispatch();
  const { items, searchResults, loading, error } = useSelector((state) => state.products);
  const [searchQuery, setSearchQuery] = React.useState('');

  const products = searchResults.length > 0 ? searchResults : items;

  useEffect(() => {
    if (items.length === 0) {
      dispatch(fetchProducts());
    }
  }, [dispatch, items.length]);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      dispatch(searchProducts(searchQuery));
    }
  };

  const clearSearch = () => {
    setSearchQuery('');
    dispatch({ type: 'products/clearSearchResults' });
  };

  if (loading) return <div>Chargement...</div>;
  if (error) return <div>Erreur: {error}</div>;

  return (
    <div className="product-list">
      <h2>Produits</h2>
      <form onSubmit={handleSearch} className="search-form">
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Rechercher des produits..."
        />
        <button type="submit">Rechercher</button>
        {searchResults.length > 0 && (
          <button type="button" onClick={clearSearch}>Effacer</button>
        )}
      </form>
      <div className="products-grid">
        {products.map((product) => (
          <div key={product.id} className="product-card">
            <h3>{product.name}</h3>
            <p>{product.description}</p>
            <p className="price">{product.price}€</p>
            <p>Stock: {product.stock}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ProductList;
